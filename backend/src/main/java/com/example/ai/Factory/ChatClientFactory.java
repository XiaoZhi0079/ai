package com.example.ai.Factory;

import com.example.ai.config.ChatModelProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatClientFactory{

    private final ChatModelProperties chatModelProperties;
    @Value("${prompt.Default_System}")
    private String defaultSystem;

    // 线程安全的本地缓存，确保每个模型只被初始化一次
    private final Map<String, ChatClient> clientCache = new ConcurrentHashMap<>();

    /**
     * 根据模型名称动态获取或创建 ChatClient
     */
    public ChatClient getClient(String modelName) {
        return clientCache.computeIfAbsent(modelName, this::createChatClient);
    }

    private ChatClient createChatClient(String modelName) {
        // 1. 在配置中查找对应的模型配置
        ChatModelProperties.Platform targetPlatform = null;
        ChatModelProperties.Platform.Options targetOptions = null;

        for (ChatModelProperties.Platform platform : chatModelProperties.getPlatforms()) {
            targetOptions = platform.getOptions().stream()
                    .filter(opt -> opt.getModel().equals(modelName))
                    .findFirst()
                    .orElse(null);

            if (targetOptions != null) {
                targetPlatform = platform;
                break;
            }
        }

        if (targetPlatform == null || targetOptions == null) {
            throw new IllegalArgumentException("未找到模型配置: " + modelName);
        }

        log.info("正在首次初始化模型实例 [懒加载]: {}", modelName);

        // 2. 创建真正的模型实例
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(targetPlatform.getBaseUrl())
                .apiKey(targetPlatform.getApiKey())
                .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(targetOptions.getModel())
                        .temperature(targetOptions.getTemperature())
                        .maxTokens(targetOptions.getMaxTokens())
                        .build())
                .build();

        // 3. 构建并返回 ChatClient
        return ChatClient.builder(chatModel)
                .defaultSystem(defaultSystem)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}
