ALTER TABLE rag_documents
    ADD COLUMN owner_user_id INT NULL COMMENT '私有知识库归属 user_id，公有库为空' AFTER uploaded_by,
    ADD COLUMN knowledge_scope VARCHAR(16) NOT NULL DEFAULT 'PRIVATE' COMMENT '知识库范围: PUBLIC/PRIVATE' AFTER owner_user_id;

UPDATE rag_documents
SET owner_user_id = uploaded_by,
    knowledge_scope = 'PRIVATE'
WHERE owner_user_id IS NULL;

CREATE TABLE IF NOT EXISTS rag_ocr_user_settings (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL UNIQUE COMMENT '用户ID',
    base_url    VARCHAR(255) NULL COMMENT '用户自定义OCR baseUrl',
    api_key     VARCHAR(255) NULL COMMENT '用户自定义OCR apiKey',
    model       VARCHAR(128) NULL COMMENT '用户自定义OCR模型名',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户OCR模型设置';
