package com.example.ai.control;


import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.example.ai.service.ChatService;
import com.example.ai.service.VideoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@Tag(name = "聊天控制类")
@RequestMapping("/ai")
public class ChatControl {

    @Autowired
    private ChatService chatservice;

    @Autowired
    private VideoService videoService;


    @Operation(summary = "文字对话")
    @RequestMapping("/chat/{model}")
    public String chattext(@PathVariable String model,
                           @RequestParam(defaultValue = "您是一个有用的助手") String defaultSystem,
                           @RequestParam String chatId,
                           @RequestParam(required = false) String userinput,
                           @RequestPart(required = false) MultipartFile imageFile) throws IOException {
            return chatservice.chat(model, defaultSystem, chatId, userinput, imageFile);
    }

    @Operation(summary = "流式文字对话")
    @RequestMapping(value="/chatstream/{model}",produces = "text/html;charset=utf-8")
    public Flux<String> chattextstream(@PathVariable String model,
                                       @RequestParam(defaultValue = "您是一个有用的助手") String defaultSystem,
                                       @RequestParam String chatId,
                                       @RequestParam String userinput) {
        return chatservice.chatstream(model,defaultSystem,chatId,userinput);
    }



    @Operation(summary = "视频对话")
    @GetMapping(value = "/video", produces = "text/html")
    public ResponseEntity<String> video(@RequestParam String userinput,
                                        @RequestParam(defaultValue="wan2.5-i2v-preview") String model,
                                        @RequestParam(defaultValue="1280*720")String size,
                                        @RequestParam(required = false) MultipartFile imageFile) throws NoApiKeyException, InputRequiredException, IOException {

            // 调用大模型，返回视频访问地址
            String url = videoService.video(userinput, model, size, imageFile);
            // 加个video标签是为了方便在浏览器查看，否则直接访问地址的话就会将视频直接下载下来
        String html = """
    <!doctype html>
    <meta charset="utf-8">
    <title>Preview</title>
    <video controls preload="metadata" style="max-width:100%%;height:auto" src="%s"></video>
    """.formatted(url);
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }


}
