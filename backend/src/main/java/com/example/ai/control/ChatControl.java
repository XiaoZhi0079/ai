package com.example.ai.control;

import com.example.ai.pojo.ChatEntity;
import com.example.ai.service.impl.ChatServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@Tag(name = "聊天控制类")
@RequestMapping("/ai")
public class ChatControl {

    @Autowired
    private ChatServiceImpl chatService;

    @Operation(summary = "文本对话")
    @RequestMapping("/chat")
    public String chattext(@RequestBody ChatEntity chatEntity) throws IOException {
        System.out.println("Received ChatEntity: " + chatEntity);
        return chatService.chat(chatEntity);
    }

//    @Operation(summary = "流式文本对话")
//    @RequestMapping(value="/chatstream",produces = "text/html;charset=utf-8")
//    public Flux<String> chattextstream(ChatEntity chatEntity) throws IOException {
//        return chatservice.chatstream(chatEntity);
//    }

    /**
     * 统一的聊天接口
     * @param chatEntity 包含消息和是否使用知识库的标志
     */
    @PostMapping("/send")
    public void chat(@RequestBody ChatEntity chatEntity) throws IOException {
        chatService.chat(chatEntity);
    }
}
