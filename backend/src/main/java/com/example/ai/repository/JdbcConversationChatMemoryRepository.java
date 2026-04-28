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
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;

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
        // Replace existing messages to avoid duplicate history growth when the full context is saved each time.
        jdbcTemplate.update("DELETE FROM messages WHERE conversation_id = ?", conversationPk);
        int nextSequence = 1;
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

    private String normalizeMessageType(MessageType type) {
        if (type == null) {
            return MessageType.ASSISTANT.getValue();
        }
        return type.getValue();
    }

    private Message toMessage(String sender, String content, String imageUrl, String mediaMeta) {
        MessageType type = resolveMessageType(sender);
        String safeText = content == null ? "" : content;
        List<Media> mediaList = buildMediaList(imageUrl, mediaMeta);
        return switch (type) {
            case USER -> mediaList.isEmpty()
                    ? new UserMessage(safeText)
                    : UserMessage.builder().text(safeText).media(mediaList).build();
            case ASSISTANT -> mediaList.isEmpty()
                    ? new AssistantMessage(safeText)
                    : AssistantMessage.builder().content(safeText).media(mediaList).build();
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
        List<String> urls = new ArrayList<>();
        for (Media media : mediaContent.getMedia()) {
            String url = resolveMediaUrl(media);
            if (url != null) {
                urls.add(url);
            }
        }
        if (urls.isEmpty()) {
            return null;
        }
        return String.join(",", urls);
    }

    private String resolveMediaUrl(Media media) {
        Object data = media.getData();
        if (data instanceof URI uri) {
            return uri.toString();
        }
        if (data instanceof java.net.URL url) {
            return url.toString();
        }
        if (data instanceof String str && str.startsWith("http")) {
            return str;
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
                String url = resolveMediaUrl(media);
                if (url != null) {
                    m.put("uri", url);
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

    private List<Media> buildMediaList(String imageUrl, String mediaMeta) {
        List<Media> mediaList = new ArrayList<>();
        if (StringUtils.hasText(mediaMeta)) {
            try {
                JsonNode root = objectMapper.readTree(mediaMeta);
                JsonNode media = root.path("media");
                if (media.isArray()) {
                    for (JsonNode node : media) {
                        String uri = node.path("uri").asText(null);
                        if (!StringUtils.hasText(uri)) continue;
                        String mt = node.path("mimeType").asText(null);
                        MimeType mimeType = StringUtils.hasText(mt)
                                ? MimeTypeUtils.parseMimeType(mt)
                                : MimeTypeUtils.APPLICATION_OCTET_STREAM;
                        mediaList.add(new Media(mimeType, URI.create(uri)));
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to parse media_meta, fallback to image_url", e);
            }
        }
        if (!mediaList.isEmpty()) {
            return mediaList;
        }
        if (!StringUtils.hasText(imageUrl)) {
            return List.of();
        }
        String[] parts = imageUrl.split(",");
        for (String part : parts) {
            String url = part.trim();
            if (!url.isEmpty()) {
                mediaList.add(new Media(MimeTypeUtils.APPLICATION_OCTET_STREAM, URI.create(url)));
            }
        }
        return mediaList;
    }
}
