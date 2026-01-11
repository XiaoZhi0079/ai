package com.example.ai.repository;

import org.springframework.ai.chat.messages.Message;


import java.util.List;


public interface ChatHistoryRepository {


    // 根据类型保存会话id
    // 类型：chat，rag
    void save(String chatId,String type);
    //根据类型获取所有会话id
    List<String> get(String type);
    //根据类型删除会话id
    void delete(String type);
    //根据类型删除所有会话id
    void deleteAll(String type);

    List<Message> getbyid(String chatId);

}
