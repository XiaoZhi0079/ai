package com.example.ai.pojo;

import lombok.Data;

@Data
public class RagParsePreview {

    private String fileName;
    private String extractedText;
    private boolean ocrUsed;
    private int charCount;
    private String knowledgeScope;
}
