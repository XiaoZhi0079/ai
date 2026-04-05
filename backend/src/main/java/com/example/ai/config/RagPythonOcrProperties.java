package com.example.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.rag.python-ocr")
public class RagPythonOcrProperties {

    private String baseUrl = "http://127.0.0.1:8000";
    private String parsePath = "/parse";
    private Integer timeoutSeconds = 120;
}
