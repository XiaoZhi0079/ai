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
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    // 注入由 创建的 Map<String, ChatClient> Bean
    private final Map<String,ChatClient> newchatClientconfig;

    //记忆
    private final ChatHistoryRepository chatHistoryRepository;

    //存储桶
    private final AliyunOssClientPutObject aliyunOssClientPutObject;

    //文档切分
    private final DocumentService documentService;

    //搜索引擎
    private final SearXngService searXngService;



    //Rag提示词
    @Value("${prompt.RAG_PROMPT_TEMPLATE}")
    String RAG_PROMPT_TEMPLATE;

    // 新增：为联网搜索提示词
    @Value("${prompt.INTERNET_SEARCH_PROMPT_TEMPLATE}")
    String INTERNET_SEARCH_PROMPT_TEMPLATE;



    @PostConstruct
    public void init() {
        System.out.println("newchatClientconfig size = " + newchatClientconfig.size());
        System.out.println("keys = " + newchatClientconfig.keySet());
    }


    public String chat(ChatEntity chatEntity) throws IOException {

        String chatid = chatEntity.getChatId();
        String userinput = chatEntity.getUserInput();
        MultipartFile imgfile = chatEntity.getImageFile();
        ChatMode chatMode = chatEntity.getChatMode();
        chatHistoryRepository.save(chatid, "chat");

        ChatClient chatClient = newchatClientconfig.get(chatEntity.getModel());


        //后续增加异常处理
        if (chatClient == null) {
            throw new IllegalArgumentException("ChatClient is null. Model not found for name: '" + chatEntity.getModel() + "'. Available models: " + newchatClientconfig.keySet());
        }

        return switch (chatMode) {
            case INTERNET_SEARCH -> internetSearch(chatEntity, chatClient, chatid, userinput, imgfile);
            case KNOWLEDGE_BASE  -> knowledgeBase(chatEntity, chatClient, chatid, userinput, imgfile);
            case DIRECT          -> direct(chatEntity, chatClient, chatid, userinput, imgfile);
        };

//        if (chatMode.equals("INTERNET_SEARCH")) {
//           return ......
//        } else if (chatMode.equals("KNOWLEDGE_BASE")) {
//           return  ......
//        } else if (chatMode.equals("DIRECT")) {
//            return chatClient.prompt("你是一个乐于助人的模型")
//                    .tools(new DateTimeTools())
//                    .user(u -> {
//                        if (userinput != null && !userinput.isBlank()) {
//                            u.text(userinput);
//                        } else {
//                            u.text("userinput");
//                        }
//                        if (chatEntity.getImageFile() != null) {
//                            String url = null;
//                            try {
//                                url = aliyunOssClientPutObject.upload(imgfile.getInputStream(), chatEntity.getImageFile().getOriginalFilename());
//                            } catch (IOException e) {
//                                throw new RuntimeException(e);
//                            }
//                            try {
//                                u.media(MimeTypeUtils.IMAGE_PNG, new URL(url));
//                            } catch (MalformedURLException e) {
//                                throw new RuntimeException(e);
//                            }
//                        }
//                    })
//                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatid))
//                    .call()
//                    .content();
//        } else {
//            //异常处理待续
//            return 'Error';
//        }
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




    @Override
    public String streamChat(ChatEntity chatEntity) {

        String userId = chatEntity.getChatId();
        String question = chatEntity.getUserInput();
        // 获取前端传递的模式，如果没有则默认为直接对话
        ChatMode mode = chatEntity.getChatMode() != null ? chatEntity.getChatMode() : ChatMode.DIRECT;

        Prompt prompt;

        // 使用 switch 语句根据模式选择不同的逻辑
        switch (mode) {
            case KNOWLEDGE_BASE:
                log.info("【用户: {}】正在使用【知识库模式】进行提问。", userId);
                prompt = createRagPrompt(question);
                break;

            case INTERNET_SEARCH:
                log.info("【用户: {}】正在使用【联网搜索模式】进行提问。", userId);
                prompt = createInternetSearchPrompt(question);
                break;

            case DIRECT:
            default:
                log.info("【用户: {}】正在使用【直接对话模式】进行提问。", userId);
                prompt = new Prompt(question);
                break;
        }

        ChatClient chatClient = newchatClientconfig.get(chatEntity.getModel());
        //流式sse后续再做。。。。。。
        String answer=chatClient.prompt(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatEntity.getChatId()))
                .call()
                .content();
        System.out.println(answer);
        return answer;

/*        stream.doOnError(throwable -> {
                    log.error("【用户: {}】的AI流处理发生错误: {}", userId, throwable.getMessage(), throwable);
                    SSEServer.sendMsg(userId, "抱歉，服务出现了一点问题，请稍后再试。", SSEMsgType.FINISH);
                    SSEServer.close(userId);
                })
                .subscribe(
                        content -> SSEServer.sendMsg(userId, content, SSEMsgType.ADD),
                        error -> log.error("【用户: {}】的流订阅最终失败: {}", userId, error.getMessage()),
                        () -> {
                            log.info("【用户: {}】的流已成功结束。", userId);
                            SSEServer.sendMsg(userId, "done", SSEMsgType.FINISH);
                            SSEServer.close(userId);
                        }
                );*/
    }


    /**
     * 创建 RAG (知识库) 模式的 Prompt
     */
    private Prompt createRagPrompt(String question) {
        List<Document> relatedDocs = documentService.doSearch(question);
        String context = "没有找到相关的知识库信息。";
        if (!CollectionUtils.isEmpty(relatedDocs)) {
            context = relatedDocs.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n---\n"));
        }
        String promptContent = RAG_PROMPT_TEMPLATE
                .replace("{context}", context)
                .replace("{question}", question);
        return new Prompt(promptContent);
    }

    /**
     * 创建联网搜索模式的 Prompt
     */
    private Prompt createInternetSearchPrompt(String question) {
        // 1. 执行联网搜索
        List<SearchResult> searchResults = searXngService.search(question);
        String context = "未能获取到有效的网络搜索结果。";

        // 2. 构建上下文
        if (!CollectionUtils.isEmpty(searchResults)) {
            // 将搜索结果格式化为清晰的上下文文本
            context = searchResults.stream()
                    .map(result -> String.format("【来源标题】: %s\n【内容摘要】: %s\n【链接】: %s",
                            result.getTitle(),
                            result.getContent(),
                            result.getUrl()))
                    .collect(Collectors.joining("\n\n---\n\n"));
        }

        // 3. 创建提示词
        String promptContent = INTERNET_SEARCH_PROMPT_TEMPLATE
                .replace("{context}", context)
                .replace("{question}", question);
        return new Prompt(promptContent);
    }


}