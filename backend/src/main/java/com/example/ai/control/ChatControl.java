package com.example.ai.control;

import com.example.ai.pojo.ChatEntity;
import com.example.ai.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

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

    @Operation(summary = "Chat Stream")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@RequestBody ChatEntity chatEntity, HttpServletRequest request) {
        Long userId = null;
        String authUserId = (String) request.getAttribute("authUserId");
        if (authUserId != null) {
            try {
                userId = Long.parseLong(authUserId);
            } catch (NumberFormatException ignored) {
                log.debug("Invalid authUserId in request: {}", authUserId);
            }
        }
        return chatService.streamChat(chatEntity, userId)
                .map(chunk -> event("chunk", chunk))
                .startWith(event("start", chatEntity.getChatId()))
                .concatWithValues(event("done", chatEntity.getChatId()))
                .onErrorResume(ex -> {
                    log.error("Chat stream controller failed: chatId={}, model={}, mode={}",
                            chatEntity.getChatId(),
                            chatEntity.getModel(),
                            chatEntity.getChatMode(),
                            ex);
                    return Flux.just(event("error", ex.getMessage() == null ? "Stream failed" : ex.getMessage()));
                });
    }

    private ServerSentEvent<String> event(String event, String data) {
        return ServerSentEvent.<String>builder()
                .event(event)
                .data(data == null ? "" : data)
                .build();
    }
}
