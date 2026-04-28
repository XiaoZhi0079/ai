package com.example.ai.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RagOcrUserSettings {

    private Integer id;
    private Integer userId;
    private String baseUrl;
    private String apiKey;
    private String model;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
