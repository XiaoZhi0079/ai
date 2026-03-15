    /* 会话记忆表结构调整（与 Spring AI 对话记忆兼容） */

ALTER TABLE conversations
    MODIFY COLUMN user_id INT NULL COMMENT '用户ID(关联users.id)';

ALTER TABLE conversations
    ADD COLUMN conversation_uid VARCHAR(36) NOT NULL UNIQUE COMMENT '外部chatId';

ALTER TABLE conversations
    ADD COLUMN type VARCHAR(16) NOT NULL DEFAULT 'chat' COMMENT '会话类型: chat/rag/other';

CREATE INDEX idx_conversations_type ON conversations (type);

ALTER TABLE messages
    MODIFY COLUMN sender VARCHAR(20) NOT NULL COMMENT '发送者(USER/ASSISTANT/SYSTEM/TOOL)';

ALTER TABLE messages
    ADD COLUMN media_meta TEXT NULL COMMENT '媒体扩展信息(JSON)';

CREATE INDEX idx_messages_conv_seq ON messages (conversation_id, sequence);
