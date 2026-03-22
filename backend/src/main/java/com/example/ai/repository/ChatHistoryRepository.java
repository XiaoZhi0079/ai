package com.example.ai.repository;

import com.example.ai.pojo.ConversationItem;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

public interface ChatHistoryRepository {

    // 保存会话（含 userId 和 title）
    void save(String chatId, String type, Long userId, String title);

    // 获取指定用户的所有会话
    List<ConversationItem> getByUserId(Long userId);

    // 根据类型删除会话id
    void delete(String type);

    // 根据类型删除所有会话id
    void deleteAll(String type);

    List<Message> getbyid(String chatId);
}
