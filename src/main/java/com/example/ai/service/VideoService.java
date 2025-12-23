package com.example.ai.service;

import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesis;
import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesisParam;
import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesisResult;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.example.ai.utils.AliyunOssClientPutObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.lang.Nullable;
import java.io.IOException;

@Service
public class VideoService {

    @Autowired
    private AliyunOssClientPutObject aliyunOssClientPutObject;

    public String video(String message,String model,String size,MultipartFile imageFile)
            throws NoApiKeyException, InputRequiredException, IOException {

        String imgUrl = aliyunOssClientPutObject.upload(imageFile.getInputStream(), imageFile.getOriginalFilename());

        VideoSynthesis vs = new VideoSynthesis();

        VideoSynthesisParam param =
                VideoSynthesisParam.builder()
                        .apiKey("sk-0ff8034547cc4716a2ac8dfd7618e897")
                        // 设置视频合成模型
                        .model(model)
                        // 设置对话
                        .prompt(message)
                        .imgUrl(imgUrl)
                        // 设置分辨率大小
                        .size(size)
                        .build();
        System.out.println("please wait...");

        // 调用大模型
        VideoSynthesisResult result = vs.call(param);
        System.out.println(JsonUtils.toJson(result));
        // 返回视频地址
        return result.getOutput().getVideoUrl();
    }
}