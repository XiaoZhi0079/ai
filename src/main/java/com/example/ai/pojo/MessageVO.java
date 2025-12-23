package com.example.ai.pojo;


import lombok.Data;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;


@Data
public class MessageVO {
    MessageType role;
    String content;
    public MessageVO(Message  message) {
        this.role = message.getMessageType();
        this.content = message.getText();
    }
}
