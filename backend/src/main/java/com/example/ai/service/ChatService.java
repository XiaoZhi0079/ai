package com.example.ai.service;

import com.example.ai.pojo.ChatEntity;

import java.io.IOException;

public interface ChatService {

    public String chat(ChatEntity chatEntity) throws IOException;
}
