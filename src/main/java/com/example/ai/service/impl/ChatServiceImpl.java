package com.example.ai.service.impl;

import com.example.ai.Tool.DateTimeTools;
import com.example.ai.enums.ChatMode;
import com.example.ai.pojo.ChatEntity;
import com.example.ai.pojo.SearchResult;
import com.example.ai.repository.ChatHistoryRepository;
import com.example.ai.service.ChatService;
import com.example.ai.service.DocumentService;
import com.example.ai.service.SearXngService;
import com.example.ai.utils.AliyunOssClientPutObject;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    // 优化命名：chatClientMap 更直观
    private final Map<String, ChatClient> chatClientMap;
    private final ChatHistoryRepository chatHistoryRepository;
    private final AliyunOssClientPutObject aliyunOssClientPutObject;
    private final DocumentService documentService;
    private final SearXngService searXngService;

    @Value("${prompt.RAG_PROMPT_TEMPLATE}")
    private String ragPromptTemplateStr;

    @Value("${prompt.INTERNET_SEARCH_PROMPT_TEMPLATE}")
    private String internetSearchPromptTemplateStr;

    @PostConstruct
    public void init() {
        log.info("已加载 ChatClients, 可用模型: {}", chatClientMap.keySet());
    }

    private record MediaResource(MimeType mimeType, URL url) {}

    @Override
    public String chat(ChatEntity chatEntity) {
        String chatId = chatEntity.getChatId();
        String modelName = chatEntity.getModel();
        
        // 记录历史（建议检查这里具体保存了什么，通常应该只保存会话元数据，内容由ChatMemory处理）
        chatHistoryRepository.save(chatId, "chat");

        ChatClient chatClient = chatClientMap.get(modelName);
        if (chatClient == null) {
            throw new IllegalArgumentException(String.format("模型不存在: '%s'。可用模型: %s", modelName, chatClientMap.keySet()));
        }

        // 1. 根据模式构建 Prompt 对象
        Prompt prompt = buildPromptByMode(chatEntity);

        // 2. 准备图片媒体信息（如果有）
        MediaResource media = uploadAndPrepareMedia(chatEntity.getImageFile());

        // 3. 调用 AI
        return chatClient.prompt(prompt)
                .tools(new DateTimeTools()) // 如果 DateTimeTools 无状态，建议注册为 Bean 注入，避免每次 new
                .user(u -> {
                    // 如果有 PromptTemplate 生成的内容，这里不需要再 set text，Spring AI 会自动合并
                    // 但这里主要是为了附加多模态图片
                    if (media != null) {
                        u.media(media.mimeType(), media.url());
                    }
                })
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .content();
    }

    /**
     * 根据聊天模式构建基础 Prompt
     */
    private Prompt buildPromptByMode(ChatEntity chatEntity) {
        String userName = chatEntity.getUserName();
        String userInput = chatEntity.getUserInput();
        ChatMode chatMode = chatEntity.getChatMode();

        switch (chatMode) {
            case INTERNET_SEARCH:
                return createInternetSearchPrompt(userName, userInput);
            case KNOWLEDGE_BASE:
                return createKnowledgeBasePrompt(userName, userInput);
            case DIRECT:
                return createDirectPrompt(userName, userInput);
            default:
                throw new IllegalArgumentException("不支持的聊天模式: " + chatMode);
        }
    }

    private Prompt createKnowledgeBasePrompt(String userId, String userInput) {
        log.info("【用户: {}】使用【知识库模式】", userId);
        List<Document> relatedDocs = documentService.doSearch(userInput);

        String context = CollectionUtils.isEmpty(relatedDocs) ?
                "没有找到相关的知识库信息。" :
                relatedDocs.stream().map(Document::getText).collect(Collectors.joining("\n---\n"));

        // 优化：使用 PromptTemplate
        PromptTemplate template = new PromptTemplate(ragPromptTemplateStr);
        return template.create(Map.of("context", context, "question", userInput));
    }

    private Prompt createInternetSearchPrompt(String userId, String userInput) {
        log.info("【用户: {}】使用【联网搜索模式】", userId);
        List<SearchResult> searchResults = searXngService.search(userInput);

        String context = CollectionUtils.isEmpty(searchResults) ?
                "未能获取到有效的网络搜索结果。" :
                searchResults.stream()
                        .map(r -> String.format("【标题】: %s\n【摘要】: %s\n【链接】: %s", r.getTitle(), r.getContent(), r.getUrl()))
                        .collect(Collectors.joining("\n\n---\n\n"));

        // 优化：使用 PromptTemplate
        PromptTemplate template = new PromptTemplate(internetSearchPromptTemplateStr);
        return template.create(Map.of("context", context, "question", userInput));
    }

    private Prompt createDirectPrompt(String userId, String userInput) {
        log.info("【用户: {}】使用【普通模式】", userId);
        // 如果需要 System Prompt 可以在这里添加，目前保持纯用户输入
        return new Prompt(new UserMessage(userInput));
    }

    /**
     * 处理图片上传并生成 MediaResource 对象
     */
    private MediaResource uploadAndPrepareMedia(MultipartFile imgFile) {
        if (imgFile == null || imgFile.isEmpty()) {
            return null;
        }
        try {
            // 1. 上传图片
            String urlStr = aliyunOssClientPutObject.upload(imgFile.getInputStream(), imgFile.getOriginalFilename());

            // 2. 动态检测 MimeType，默认为 PNG
            MimeType mimeType = MimeTypeUtils.IMAGE_PNG;
            if (imgFile.getContentType() != null) {
                try {
                    mimeType = MimeTypeUtils.parseMimeType(imgFile.getContentType());
                } catch (Exception e) {
                    log.warn("无法解析 MIME 类型: {}, 使用默认 PNG", imgFile.getContentType());
                }
            }

            return new MediaResource(mimeType, new URL(urlStr));
        } catch (IOException e) {
            log.error("图片处理失败", e);
            throw new RuntimeException("图片上传失败", e);
        }
    }
}



//    public Flux<String> chatstream(ChatEntity chatEntity) {
//
//        chatHistoryRepository.save(chatEntity.getChatId(),"chat");
//
//        return newchatClientconfig.get(chatEntity.getModel()).mutate()
//                        .defaultSystem(Default_System)
//                        .build()
//                .prompt()
//                .user(chatEntity.getUserInput())
//                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID,chatEntity.getChatId()))
//                .stream()
//                .content();
//    }




//    @Override
//    public String streamChat(ChatEntity chatEntity) {
//
//        stream.doOnError(throwable -> {
//                    log.error("【用户: {}】的AI流处理发生错误: {}", userId, throwable.getMessage(), throwable);
//                    SSEServer.sendMsg(userId, "抱歉，服务出现了一点问题，请稍后再试。", SSEMsgType.FINISH);
//                    SSEServer.close(userId);
//                })
//                .subscribe(
//                        content -> SSEServer.sendMsg(userId, content, SSEMsgType.ADD),
//                        error -> log.error("【用户: {}】的流订阅最终失败: {}", userId, error.getMessage()),
//                        () -> {
//                            log.info("【用户: {}】的流已成功结束。", userId);
//                            SSEServer.sendMsg(userId, "done", SSEMsgType.FINISH);
//                            SSEServer.close(userId);
//                        }
//                );
//    }
