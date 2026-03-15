package com.example.ai.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.content.MediaContent;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@Primary
@RequiredArgsConstructor
public class JdbcConversationChatMemoryRepository implements ChatMemoryRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<String> findConversationIds() {
        return jdbcTemplate.query(
                "SELECT conversation_uid FROM conversations ORDER BY updated_time DESC",
                (rs, rowNum) -> rs.getString("conversation_uid")
        );
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        Long conversationPk = findConversationPk(conversationId);
        if (conversationPk == null) {
            return List.of();
        }
        String sql = """
                SELECT sender, content, image_url, media_meta
                FROM messages
                WHERE conversation_id = ?
                ORDER BY sequence ASC, id ASC
                """;
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> toMessage(
                        rs.getString("sender"),
                        rs.getString("content"),
                        rs.getString("image_url"),
                        rs.getString("media_meta")
                ),
                conversationPk
        );
    }

    @Override
    @Transactional
    public void saveAll(String conversationId, List<Message> messages) {
        if (conversationId == null || conversationId.isBlank() || messages == null || messages.isEmpty()) {
            return;
        }
        Long conversationPk = ensureConversation(conversationId);
        if (conversationPk == null) {
            return;
        }
        int nextSequence = nextSequence(conversationPk);
        String sql = """
                INSERT INTO messages (conversation_id, sender, content, image_url, media_meta, sequence)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        for (Message message : messages) {
            String sender = normalizeMessageType(message.getMessageType());
            String content = message.getText();
            String imageUrl = extractImageUrl(message);
            String mediaMeta = buildMediaMeta(message);
            jdbcTemplate.update(sql, conversationPk, sender, content, imageUrl, mediaMeta, nextSequence++);
        }
        jdbcTemplate.update("UPDATE conversations SET updated_time = NOW() WHERE id = ?", conversationPk);
    }

    @Override
    @Transactional
    public void deleteByConversationId(String conversationId) {
        Long conversationPk = findConversationPk(conversationId);
        if (conversationPk == null) {
            return;
        }
        jdbcTemplate.update("DELETE FROM messages WHERE conversation_id = ?", conversationPk);
        jdbcTemplate.update("DELETE FROM conversations WHERE id = ?", conversationPk);
    }

    private Long findConversationPk(String conversationId) {
        List<Long> ids = jdbcTemplate.query(
                "SELECT id FROM conversations WHERE conversation_uid = ?",
                (rs, rowNum) -> rs.getLong("id"),
                conversationId
        );
        return ids.isEmpty() ? null : ids.get(0);
    }

    private Long ensureConversation(String conversationId) {
        Long existing = findConversationPk(conversationId);
        if (existing != null) {
            return existing;
        }
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO conversations (conversation_uid) VALUES (?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, conversationId);
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            return key != null ? key.longValue() : findConversationPk(conversationId);
        } catch (DuplicateKeyException e) {
            return findConversationPk(conversationId);
        }
    }

    private int nextSequence(Long conversationPk) {
        Integer max = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(sequence), 0) FROM messages WHERE conversation_id = ?",
                Integer.class,
                conversationPk
        );
        return (max == null ? 0 : max) + 1;
    }

    private String normalizeMessageType(MessageType type) {
        if (type == null) {
            return MessageType.ASSISTANT.getValue();
        }
        return type.getValue();
    }

    private Message toMessage(String sender, String content, String imageUrl, String mediaMeta) {
        MessageType type = resolveMessageType(sender);
        String safeText = content == null ? "" : content;
        return switch (type) {
            case USER -> new UserMessage(safeText);
            case ASSISTANT -> new AssistantMessage(safeText);
            case SYSTEM -> new SystemMessage(safeText);
            case TOOL -> new SystemMessage(safeText);
        };
    }

    private MessageType resolveMessageType(String sender) {
        if (sender == null || sender.isBlank()) {
            return MessageType.ASSISTANT;
        }
        String normalized = sender.trim().toLowerCase();
        if ("ai".equals(normalized)) {
            return MessageType.ASSISTANT;
        }
        try {
            return MessageType.fromValue(normalized);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown sender type '{}', fallback to assistant", sender);
            return MessageType.ASSISTANT;
        }
    }

    private String extractImageUrl(Message message) {
        if (!(message instanceof MediaContent mediaContent)) {
            return null;
        }
        for (Media media : mediaContent.getMedia()) {
            Object data = media.getData();
            if (data instanceof URI uri) {
                return uri.toString();
            }
        }
        return null;
    }

    private String buildMediaMeta(Message message) {
        Map<String, Object> meta = new LinkedHashMap<>();
        if (message.getMetadata() != null && !message.getMetadata().isEmpty()) {
            meta.put("metadata", message.getMetadata());
        }
        if (message instanceof MediaContent mediaContent && !mediaContent.getMedia().isEmpty()) {
            List<Map<String, Object>> mediaList = new ArrayList<>();
            for (Media media : mediaContent.getMedia()) {
                Map<String, Object> m = new LinkedHashMap<>();
                if (media.getMimeType() != null) {
                    m.put("mimeType", media.getMimeType().toString());
                }
                if (media.getId() != null) {
                    m.put("id", media.getId());
                }
                if (media.getName() != null) {
                    m.put("name", media.getName());
                }
                Object data = media.getData();
                if (data instanceof URI uri) {
                    m.put("uri", uri.toString());
                } else if (data != null) {
                    m.put("dataType", data.getClass().getName());
                }
                mediaList.add(m);
            }
            if (!mediaList.isEmpty()) {
                meta.put("media", mediaList);
            }
        }
        if (meta.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(meta);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize media metadata", e);
            return null;
        }
    }
}
