package com.example.ai.pojo;

import lombok.Data;
import org.springframework.ai.chat.messages.MessageType;

import java.time.LocalDateTime;

@Data
public class MessagePojo {

    private Long id;
    private String conversationId;
    private MessageType type;
    private String content;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;
    private String mediaUrl;
    private String mediaMeta;
}
