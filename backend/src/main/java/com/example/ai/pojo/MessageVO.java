package com.example.ai.pojo;


import lombok.Data;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.content.Media;
import org.springframework.ai.content.MediaContent;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;



//SpringAi官方MessageV0
@Data
public class MessageVO {
    MessageType role;
    String content;
    List<String> images;
    public MessageVO(Message  message) {
        this.role = message.getMessageType();
        this.content = message.getText();
        this.images = extractImages(message);
    }

    private List<String> extractImages(Message message) {
        if (!(message instanceof MediaContent mediaContent)) {
            return List.of();
        }
        List<String> urls = new ArrayList<>();
        for (Media media : mediaContent.getMedia()) {
            Object data = media.getData();
            if (data instanceof URI uri) {
                urls.add(uri.toString());
            }
        }
        return urls;
    }
}
