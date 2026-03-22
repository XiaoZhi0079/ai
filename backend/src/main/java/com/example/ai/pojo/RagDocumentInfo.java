package com.example.ai.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RagDocumentInfo {

    private Integer id;
    private String fileName;
    private String ossUrl;
    private Integer uploadedBy;
    private Integer ownerUserId;
    private String knowledgeScope;
    private LocalDateTime createdAt;
}
