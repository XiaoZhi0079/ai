package com.example.ai.control;

import com.example.ai.pojo.MessageVO;
import com.example.ai.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai/history")
@RequiredArgsConstructor
public class ChatHistory {

    private final ChatHistoryRepository chatHistoryRepository;
    //    @RequestMapping("{type}")
//    public List<String> get(@PathVariable("type") String type)
//    {
//        return chatHistoryRepository.get(type);
//    }
    @GetMapping("type/{type}")
    public List<String> get(@PathVariable("type") String type)
    {
        return chatHistoryRepository.get(type);
    }
    @GetMapping("chat/{chatId}")
    public List<MessageVO> getbyid(@PathVariable("chatId") String chatId)
    {
        List<Message> messages = chatHistoryRepository.getbyid(chatId);
        return messages.stream().map(MessageVO::new).toList();
    }
}
