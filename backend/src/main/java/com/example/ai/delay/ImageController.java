package com.example.ai.delay;

import com.example.ai.pojo.ImagesResponse;
import com.example.ai.pojo.LeeResult;
import com.example.ai.repository.ChatHistoryRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.image.*;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/image")
public class ImageController {

    private final OpenAiImageModel imageModel;
    private final ChatMemory chatMemory;
    private final ChatHistoryRepository chatHistoryRepository;

    public ImageController(
            @Value("${ai.image.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}") String baseUrl,
            @Value("${ai.image.api-key:${ai.platforms[0].api-key}}") String apiKey,
            @Value("${ai.image.model:wan2.5-t2i-preview}") String model,
            ChatMemory chatMemory,
            ChatHistoryRepository chatHistoryRepository) {

        OpenAiImageApi openAiImageApi = OpenAiImageApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();

        OpenAiImageOptions options = OpenAiImageOptions.builder()
                .model(model)
                .width(1024)
                .height(1024)
                .build();

        this.imageModel = new OpenAiImageModel(
                openAiImageApi,
                options,
                RetryTemplate.defaultInstance()
        );
        this.chatMemory = chatMemory;
        this.chatHistoryRepository = chatHistoryRepository;
    }

    @GetMapping("/chat")
    public LeeResult<ImagesResponse> generate(
            @RequestParam(defaultValue = "A cute baby sea otter") String prompt,
            @RequestParam(required = false) String chatId,
            HttpServletRequest request) {
        try {
            ImageResponse response = imageModel.call(new ImagePrompt(prompt));
            String url = response.getResult().getOutput().getUrl();
            ImagesResponse imagesResponse = new ImagesResponse(url, url, "image/png");

            if (StringUtils.hasText(chatId)) {
                Long userId = null;
                String authUserId = (String) request.getAttribute("authUserId");
                if (authUserId != null) {
                    try { userId = Long.parseLong(authUserId); } catch (NumberFormatException ignored) {}
                }
                String title = StringUtils.hasText(prompt) ? (prompt.length() > 50 ? prompt.substring(0, 50) : prompt) : null;
                chatHistoryRepository.save(chatId, "DIRECT", userId, title);
                UserMessage userMessage = new UserMessage(prompt);
                AssistantMessage assistantMessage = AssistantMessage.builder()
                        .content("生成的图片")
                        .media(List.of(new Media(MimeTypeUtils.APPLICATION_OCTET_STREAM, URI.create(url))))
                        .build();
                chatMemory.add(chatId, List.of(userMessage, assistantMessage));
            }

            return LeeResult.ok(imagesResponse);
        } catch (Exception e) {
            log.error("图片生成失败, prompt: {}", prompt, e);
            return LeeResult.fail("生成失败：" + e.getMessage());
        }
    }
}
