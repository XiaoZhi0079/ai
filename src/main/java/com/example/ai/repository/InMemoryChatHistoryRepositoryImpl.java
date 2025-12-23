package com.example.ai.repository;


import org.springframework.ai.chat.memory.ChatMemory;

import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class InMemoryChatHistoryRepositoryImpl implements ChatHistoryRepository {

    @Autowired
    private ChatMemory chatMemory;

    private final Map<String,List<String>> chathistory=new HashMap<>();



    //存储对话形式与对话id的方法
    @Override
    public void save(String chatId, String type) {

//        System.out.println("执行sava方法");
//     if (!chathistory.containsKey(type)){
//         chathistory.put(type, new ArrayList<>());
//     }
//     List<String> chatids=chathistory.get(type);
        List<String> chatids=chathistory.computeIfAbsent(type, k -> new ArrayList<>());
        if (chatids.contains(chatId)){
            return;
        }
        chatids.add(chatId);
    }

    //根据类型获取所有对话id
    @Override
    public List<String> get(String type) {
//        if (!chathistory.containsKey(type)){
//            return new ArrayList<>();
//        }
//        List<String> chatids=chathistory.get(type);
//        return chatids;
        return chathistory.getOrDefault(type, new ArrayList<>());
    }


    //删除对话
    @Override
    public void delete(String type) {

    }


    //删除所有对话
    @Override
    public void deleteAll(String type) {

    }

    //根据id获取对话信息
    @Override
    public List<Message> getbyid(String chatId) {
        return chatMemory.get(chatId);
    }
}
