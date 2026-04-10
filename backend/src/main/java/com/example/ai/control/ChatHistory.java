package com.example.ai.control;

import com.example.ai.pojo.ConversationItem;
import com.example.ai.pojo.MessageVO;
import com.example.ai.repository.ChatHistoryRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai/history")
@RequiredArgsConstructor
public class ChatHistory {

    private final ChatHistoryRepository chatHistoryRepository;

    @GetMapping
    public List<ConversationItem> get(HttpServletRequest request) {
        Long userId = resolveUserId(request);
        return chatHistoryRepository.getByUserId(userId);
    }

    @GetMapping("chat/{chatId}")
    public List<MessageVO> getbyid(@PathVariable("chatId") String chatId) {
        List<Message> messages = chatHistoryRepository.getbyid(chatId);
        return messages.stream().map(MessageVO::new).toList();
    }

    @DeleteMapping("chat/{chatId}")
    public ResponseEntity<Void> deleteChat(@PathVariable("chatId") String chatId, HttpServletRequest request) {
        Long userId = resolveUserId(request);
        boolean deleted = chatHistoryRepository.deleteByChatId(userId, chatId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    private Long resolveUserId(HttpServletRequest request) {
        String authUserId = (String) request.getAttribute("authUserId");
        if (authUserId != null) {
            try {
                return Long.parseLong(authUserId);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
