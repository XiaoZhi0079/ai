package com.example.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.rag.ocr")
public class RagOcrProperties {

    private String baseUrl;
    private String apiKey;
    private String model;
    private Integer maxTokens = 4096;
    private Integer pdfDpi = 160;
    private Integer maxPages = 20;
    private Integer maxEmbeddedImages = 10;
    private String prompt = "请执行 OCR，只输出图片中的完整文字内容。不要总结，不要解释，不要补充。保留原有段落和换行；如果没有识别到文字，返回空字符串。";
}
