## 复盘要点
当前方案的方向是对的：复用 `建表.sql` 的 `conversations/messages`，并用自定义 JDBC Repository 接入 Spring AI。  
但存在三处可优化点：
- `messages` 再加 `conversation_uid` 会造成冗余和一致性风险（两个字段可能不同步）。
- `ChatHistoryRepository.save(chatId, type)` 需要持久化字段，但 `conversations` 里没有 `type`。
- `sender` 的 enum 仅有 `USER/AI`，与 Spring AI 的 `USER/ASSISTANT/SYSTEM/TOOL` 不匹配。

下面是优化后的最终方案。

## 优化后的最终方案
**目标：** 复用现有表结构，做最小必要调整；以 `conversation_uid` 作为外部 `chatId` 映射；避免重复存储；对 Spring AI 的消息类型完全兼容。

### 1. 表结构最小调整（推荐）
**只在 `conversations` 中引入 `conversation_uid`，不在 `messages` 冗余存一份。**  
保持三表关系清晰、避免一致性问题。

**DDL 调整建议：**
```sql
-- conversations：增加外部会话 ID 和类型字段
ALTER TABLE conversations
  ADD COLUMN conversation_uid VARCHAR(36) NOT NULL UNIQUE COMMENT '外部 chatId',
  ADD COLUMN type VARCHAR(16) NOT NULL DEFAULT 'chat' COMMENT '会话类型: chat/rag/other';

-- messages：放宽 sender 以兼容 Spring AI 消息类型
ALTER TABLE messages
  MODIFY COLUMN sender VARCHAR(20) NOT NULL COMMENT 'USER/ASSISTANT/SYSTEM/TOOL';

-- messages：补充媒体元信息（可选但推荐）
ALTER TABLE messages
  ADD COLUMN media_meta TEXT NULL COMMENT '媒体扩展信息(JSON)';

-- 索引优化
CREATE UNIQUE INDEX ux_conversations_uid ON conversations (conversation_uid);
CREATE INDEX idx_messages_conv_seq ON messages (conversation_id, sequence);
```

### 2. 存储语义与映射规则
- `chatId`（String） -> `conversations.conversation_uid`
- `messages.conversation_id` -> `conversations.id`
- `MessageType` 映射：
  - `USER` -> `sender = 'USER'`
  - `ASSISTANT` -> `sender = 'ASSISTANT'`
  - `SYSTEM` -> `sender = 'SYSTEM'`
  - `TOOL` -> `sender = 'TOOL'`
- 多模态：
  - 文本写 `messages.content`
  - 图片写 `messages.image_url`
  - 其他元信息写 `messages.media_meta`（JSON）

### 3. 自定义 ChatMemoryRepository（核心实现）
**原因：** Spring AI 默认 JDBC 表结构与现有表不一致，直接启用会导致双表、双写。

实现建议（接口维持 Spring AI 语义）：
- `add(conversationId, messages)`
  - 查询/创建 `conversations` 记录（以 `conversation_uid` 唯一）
  - 计算 `sequence = MAX(sequence)+1`（建议在事务里 `SELECT ... FOR UPDATE`）
  - 插入 `messages`，更新 `conversations.updated_time`
- `get(conversationId)`
  - 通过 `conversation_uid` -> `conversation_id` 查询
  - 按 `sequence` 升序读出，组装为 Spring AI `Message`
- `clear(conversationId)`
  - 物理删除该会话 `messages`，可选择保留 `conversations` 记录

### 4. ChatHistoryRepository 持久化对齐
`ChatHistoryRepository` 需要 `type` 字段支持，因此建议直接使用 `conversations.type`：
- `save(chatId, type)`：若无记录则插入；有则更新 `updated_time`
- `get(type)`：查 `conversations` 中对应类型的 `conversation_uid` 列表
- `getbyid(chatId)`：委托 `ChatMemory` 拉取消息

### 5. 配置建议
关闭 Spring AI 自动建表，避免生成默认表：
```
spring.ai.chat.memory.repository.jdbc.initialize-schema=never
```

### 6. 迁移与兼容
- 若数据库已有 `conversations` 数据：  
  给已有记录补充 `conversation_uid`（可用 UUID），旧数据仍可通过 `conversation_id` 查询。
- 当前系统未见旧数据落库，迁移成本低。

## 方案产出清单（最终）
- SQL：调整 `conversations/messages` 字段与索引（见上方 DDL）
- Java：新增自定义 `ChatMemoryRepository`（JDBC）
- Java：`ChatHistoryRepository` 改为数据库实现
- 配置：关闭 Spring AI 默认建表

## 结果
该方案保持现有表结构为核心数据源，避免重复表；  
兼容 Spring AI 所有消息类型；  
支持会话持久化、可检索、可复现上下文；  
与现有 `chatId` 使用方式完全一致，改动最小、风险可控。
