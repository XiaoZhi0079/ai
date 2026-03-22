package com.example.ai.pojo;

import lombok.Data;

@Data
public class RagOcrRequestConfig {

    private String baseUrl;
    private String apiKey;
    private String model;
}
