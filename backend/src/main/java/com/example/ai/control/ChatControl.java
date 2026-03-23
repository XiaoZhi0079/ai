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

/**
 * Exposes the chat endpoint and resolves the current authenticated user.
 */
@Slf4j
@RestController
@Tag(name = "Chat Controller")
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ChatControl {

    private final ChatService chatService;

    @Operation(summary = "Chat")
    @PostMapping("/chat")
    public String chat(@RequestBody ChatEntity chatEntity, HttpServletRequest request) throws IOException {
        log.debug("Received chat request: chatId={}, model={}, mode={}",
                chatEntity.getChatId(),
                chatEntity.getModel(),
                chatEntity.getChatMode());

        // The JWT interceptor stores the current user id in the request.
        Long userId = null;
        String authUserId = (String) request.getAttribute("authUserId");
        if (authUserId != null) {
            try {
                userId = Long.parseLong(authUserId);
            } catch (NumberFormatException ignored) {
                log.debug("Invalid authUserId in request: {}", authUserId);
            }
        }
        return chatService.chat(chatEntity, userId);
    }
}
