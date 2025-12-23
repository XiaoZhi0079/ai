package com.example.ai.control;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.image.*;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/image")
public class ImageController {

    private final OpenAiImageModel imageModel;

    public ImageController() {
        // 初始化一次模型
        OpenAiImageApi openAiImageApi = OpenAiImageApi.builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .apiKey("sk-0ff8034547cc4716a2ac8dfd7618e897")
                .build();

        OpenAiImageOptions options = OpenAiImageOptions.builder()
                .model("wan2.5-t2i-preview")
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
            e.printStackTrace();
            return "生成失败：" + e.getMessage();
        }
    }
}

//@RestController
//public class ImageController {
//    private final ImageModel imageModel;
//
//    ImageModelController(ImageModel imageModel) {
//        this.imageModel = imageModel;
//    }
//
//    @RequestMapping("/image")
//    public String image(String input) {
//        ImageOptions options = ImageOptionsBuilder.builder()
//                .withModel("dall-e-3")
//                .build();
//
//        ImagePrompt imagePrompt = new ImagePrompt(input, options);
//        ImageResponse response = imageModel.call(imagePrompt);
//        String imageUrl = response.getResult().getOutput().getUrl();
//
//        return "redirect:" + imageUrl;
//    }
//
//    @RequestMapping("/imageWithOptions")
//    public String imageWithOptions(String input) {
//        ImageOptions options = ImageOptionsBuilder.builder()
//                .withModel("dall-e-3")
//                .withResolution("1024x1024")
//                .build();
//
//        ImagePrompt imagePrompt = new ImagePrompt(input, options);
//        ImageResponse response = imageModel.call(imagePrompt);
//        String imageUrl = response.getResult().getOutput().getUrl();
//
//        return "redirect:" + imageUrl;
//    }
//}