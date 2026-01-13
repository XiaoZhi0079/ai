package com.example.ai.service.impl;

import com.example.ai.Tool.DateTimeTools;
import com.example.ai.enums.ChatMode;
import com.example.ai.pojo.ChatEntity;
import com.example.ai.pojo.ImagesResponse;
import com.example.ai.pojo.SearchResult;
import com.example.ai.repository.ChatHistoryRepository;
import com.example.ai.service.ChatService;
import com.example.ai.service.DocumentService;
import com.example.ai.service.SearXngService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;
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

    private record MediaResource(MimeType mimeType, URL url) {
    }

    @Override
    public String chat(ChatEntity chatEntity) {

        //获取对话ID
        String chatId = chatEntity.getChatId();
        //获取对话模型
        String modelName = chatEntity.getModel();

        // 记录历史（建议检查这里具体保存了什么，通常应该只保存会话元数据，内容由ChatMemory处理）
        chatHistoryRepository.save(chatId, "chat");

        //获取chatClient
        ChatClient chatClient = chatClientMap.get(modelName);
        if (chatClient == null) {
            throw new IllegalArgumentException(String.format("模型不存在: '%s'。可用模型: %s", modelName, chatClientMap.keySet()));
        }

        // 1. 根据模式构建 Prompt 对象
        Prompt prompt = buildPromptByMode(chatEntity);

        // 2. 准备图片媒体信息（如果有）
        List<ImagesResponse> images = (chatEntity.getImageFiles());

        // 3. 调用 AI
        return chatClient.prompt(prompt)
                /*.tools(new DateTimeTools())*/ // 如果 DateTimeTools 无状态，建议注册为 Bean 注入，避免每次 new
                .user(u -> {
                    if (images != null) {
                        for (ImagesResponse i : images) {
                            if (!StringUtils.hasText(i.getImageUrls())) continue;
                            try {
                                u.media(i.getMimeTypes(), new URL(i.getImageUrls()));
                            } catch (Exception e) {
                                // 记录日志，不要让一张坏链接把整次对话弄崩
                                 log.warn("bad image url: {}", i, e);
                            }
                        }
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

        //获取用户姓名
        String userName = chatEntity.getUserName();
        //获取用户输入
        String userInput = chatEntity.getUserInput();
        //获取对话模式
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

    /**
     * 知识库 Prompt
     */
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

    /**
     * 联网搜索 Prompt
     */
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

    /**
     * 基础 Prompt
     */
    private Prompt createDirectPrompt(String userId, String userInput) {
        log.info("【用户: {}】使用【普通模式】", userId);
        return new Prompt(new UserMessage(userInput));
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
