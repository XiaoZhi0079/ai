package com.example.ai.control;

import com.example.ai.pojo.ChatEntity;
import com.example.ai.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@Tag(name = "聊天控制类")
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ChatControl {

    private final ChatService chatService;

    @Operation(summary = "对话")
    @PostMapping("/chat")
    public String chat(@RequestBody ChatEntity chatEntity, HttpServletRequest request) throws IOException {
        log.debug("收到聊天请求: chatId={}, model={}, mode={}", chatEntity.getChatId(), chatEntity.getModel(), chatEntity.getChatMode());
        Long userId = null;
        String authUserId = (String) request.getAttribute("authUserId");
        if (authUserId != null) {
            try {
                userId = Long.parseLong(authUserId);
            } catch (NumberFormatException ignored) {
            }
        }
        return chatService.chat(chatEntity, userId);
    }
}
