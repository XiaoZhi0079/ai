package com.example.ai.config;

import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesis;
import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesisParam;
import lombok.Data;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.api.OpenAiApi;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Component
@Configuration
public class ChatModelConfig {

    private final List<ChatModelProperties> chatModelProperties;

    @Autowired
    JdbcChatMemoryRepository chatMemoryRepository;

    @Bean
    public ChatMemory chatMemory(){
        return MessageWindowChatMemory.builder().maxMessages(50).build();
    }

    @Bean
    public ChatMemory chatMemory2(){
            ChatMemory chatMemory2 = MessageWindowChatMemory.builder()
            .chatMemoryRepository(chatMemoryRepository)
            .maxMessages(50)
            .build();
            return chatMemory2;
    }


    @Bean
    public Map<String,Map<String,ChatClient>> ChatClientconfig(ChatModelProperties chatModelProperties) {
        return chatModelProperties.getPlatforms().stream().collect(
                Collectors.toMap(
                        ChatModelProperties.Platform::getName,
                        platform -> {
                            System.out.println("正在创建模型: " + platform.getName());
                            return platform.getOptions().stream().collect(
                                Collectors.toMap(
                                        ChatModelProperties.Platform.Options::getModel,
                                        options -> {
                                            //创建openaiapi对象
                                            OpenAiApi openAiApi =
                                                    OpenAiApi.builder()
                                                            .baseUrl(platform.getBaseUrl())
                                                            .apiKey(platform.getApiKey())
                                                            .build();
                                            //创建OpenAiChatModel对象
                                            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                                                    .openAiApi(openAiApi)
                                                    .defaultOptions(OpenAiChatOptions.builder()
                                                            .model(options.getModel())
                                                            .temperature(options.getTemperature())
                                                            .maxTokens(options.getMaxTokens())
                                                            .build())
                                                    .build();

                                            // 4. 创建并返回 ChatClient 实例
                                            return ChatClient.builder(chatModel)
//                                                    .defaultSystem("您是一个由 " + platform.getName() + " 提供支持的有用助手。")
                                                    .defaultAdvisors(
                                                            new SimpleLoggerAdvisor(),
                                                            MessageChatMemoryAdvisor.builder(chatMemory2()).build()
                                                    )
                                                    .build();
                                        }
                                )
                        );
                        }
                )
        );
    }

    @Bean
    public Map<String,ChatClient> NewChatClientconfig(Map<String,Map<String,ChatClient>> chatClientconfig){
        Map<String, ChatClient> flatMap = chatClientconfig.values().stream()
                .flatMap(innerMap -> innerMap.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return flatMap;
    }


//    @Bean
//    public VideoSynthesisParam VideoSynthesisParam(){
//        return VideoSynthesisParam.builder()
//                        .apiKey("sk-0ff8034547cc4716a2ac8dfd7618e897")
//                        .model("wan2.5-i2v-preview")
//                        .build();
//    }

}



