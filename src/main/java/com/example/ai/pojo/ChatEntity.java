package com.example.ai.pojo;


import com.example.ai.enums.ChatMode;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;


@Data
public class ChatEntity {

    private String userName;

    private String chatId;

    private String userInput;

    private ChatMode chatMode;

    private String model;

    private MultipartFile imageFile = null;

}
