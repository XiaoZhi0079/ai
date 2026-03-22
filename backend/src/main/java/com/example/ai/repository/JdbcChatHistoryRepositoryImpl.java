package com.example.ai.repository;

import com.example.ai.pojo.ConversationItem;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Primary
@RequiredArgsConstructor
public class JdbcChatHistoryRepositoryImpl implements ChatHistoryRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ChatMemory chatMemory;

    @Override
    public void save(String chatId, String type, Long userId, String title) {
        if (chatId == null || chatId.isBlank()) {
            return;
        }
        String normalizedType = normalizeType(type);
        String sql = """
                INSERT INTO conversations (conversation_uid, type, user_id, title, updated_time)
                VALUES (?, ?, ?, ?, NOW())
                ON DUPLICATE KEY UPDATE type = VALUES(type),
                    user_id = COALESCE(user_id, VALUES(user_id)),
                    title = COALESCE(title, VALUES(title)),
                    updated_time = NOW()
                """;
        jdbcTemplate.update(sql, chatId, normalizedType, userId, title);
    }

    @Override
    public List<ConversationItem> getByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        String sql = """
                SELECT conversation_uid, title
                FROM conversations
                WHERE user_id = ?
                ORDER BY updated_time DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new ConversationItem(
                rs.getString("conversation_uid"),
                rs.getString("title")
        ), userId);
    }

    @Override
    @Transactional
    public void delete(String type) {
        String normalizedType = normalizeType(type);
        Long id = jdbcTemplate.query(
                "SELECT id FROM conversations WHERE type = ? ORDER BY updated_time DESC LIMIT 1",
                rs -> rs.next() ? rs.getLong("id") : null,
                normalizedType
        );
        if (id == null) {
            return;
        }
        jdbcTemplate.update("DELETE FROM messages WHERE conversation_id = ?", id);
        jdbcTemplate.update("DELETE FROM conversations WHERE id = ?", id);
    }

    @Override
    @Transactional
    public void deleteAll(String type) {
        String normalizedType = normalizeType(type);
        jdbcTemplate.update(
                "DELETE FROM messages WHERE conversation_id IN (SELECT id FROM conversations WHERE type = ?)",
                normalizedType
        );
        jdbcTemplate.update("DELETE FROM conversations WHERE type = ?", normalizedType);
    }

    @Override
    public List<Message> getbyid(String chatId) {
        return chatMemory.get(chatId);
    }

    private String normalizeType(String type) {
        return (type == null || type.isBlank()) ? "chat" : type.trim();
    }
}
