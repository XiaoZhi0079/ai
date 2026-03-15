package com.example.ai.delay;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.image.*;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/image")
public class ImageController {

    private final OpenAiImageModel imageModel;

    public ImageController(
            @Value("${ai.image.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}") String baseUrl,
            @Value("${ai.image.api-key:${ai.platforms[0].api-key}}") String apiKey,
            @Value("${ai.image.model:wan2.5-t2i-preview}") String model) {

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
    }

    @GetMapping("/chat")
    public String generate(@RequestParam(defaultValue = "A cute baby sea otter") String prompt) {
        try {
            ImageResponse response = imageModel.call(new ImagePrompt(prompt));
            return response.getResult().getOutput().getUrl();
        } catch (Exception e) {
            log.error("图片生成失败, prompt: {}", prompt, e);
            return "生成失败：" + e.getMessage();
        }
    }
}
