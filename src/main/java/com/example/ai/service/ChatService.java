package com.example.ai.service;

import com.alibaba.dashscope.threads.ContentImageFile;
import com.example.ai.Tool.DateTimeTools;
import com.example.ai.repository.ChatHistoryRepository;
import com.example.ai.utils.AliyunOssClientPutObject;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

@Service
public class ChatService {

    // 注入由 MultipleChatModelConfig 创建的 Map<String, ChatClient> Bean
//    private  final Map<String, Map<String,ChatClient>> chatClientconfig;

    @Autowired
    private Map<String,ChatClient> newchatClientconfig;

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @Autowired
    private AliyunOssClientPutObject aliyunOssClientPutObject;

//    public ChatClientService1(Map<String, Map<String,ChatClient>> chatClientconfig) {
//        this.chatClientconfig = chatClientconfig;
//        System.out.println("ChatClientService 已初始化，可用模型: " + chatClientconfig.keySet());
//    }

    public String chat(String model, String defaultSystem, String chatId, @Nullable String userinput, @Nullable MultipartFile imageFile) throws IOException {

        chatHistoryRepository.save(chatId,"chat");

        return newchatClientconfig.get(model).mutate()
                .defaultSystem(defaultSystem)
                .build()
                .prompt("What day is tomorrow?")
                .tools(new DateTimeTools())
                .user(u -> {
                    if(userinput != null && !userinput.isBlank()){
                        u.text(userinput);
                    }
                    else{
                        u.text("你是ai助手");
                    }
                    if(imageFile != null){
                        String url = null;
                        try {
                            url = aliyunOssClientPutObject.upload(imageFile.getInputStream(), imageFile.getOriginalFilename());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        try {
                            u.media(MimeTypeUtils.IMAGE_PNG, new URL(url));
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                })
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .content();

    }

    public Flux<String> chatstream(String mdoel, String defaultSystem,String chatId, String userinput) {

        chatHistoryRepository.save(chatId,"chat");

        return newchatClientconfig.get(mdoel).mutate()
                        .defaultSystem(defaultSystem)
                        .build()
                .prompt()
                .user(userinput)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

}