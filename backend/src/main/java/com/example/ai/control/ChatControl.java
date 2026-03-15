package com.example.ai.control;

import com.example.ai.pojo.ChatEntity;
import com.example.ai.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@Tag(name = "聊天控制类")
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ChatControl {

    private final ChatService chatService;

    @Operation(summary = "文本对话")
    @PostMapping("/chat")
    public String chat(@RequestBody ChatEntity chatEntity) {
        log.debug("收到聊天请求: chatId={}, model={}, mode={}", chatEntity.getChatId(), chatEntity.getModel(), chatEntity.getChatMode());
        return chatService.chat(chatEntity);
    }
}
