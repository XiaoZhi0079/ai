package com.example.ai.service;

import com.example.ai.pojo.ChatEntity;

import java.io.IOException;

public interface ChatService {

    String chat(ChatEntity chatEntity, Long userId) throws IOException;
}
