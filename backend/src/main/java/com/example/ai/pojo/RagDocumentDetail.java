package com.example.ai.pojo;

import lombok.Data;

@Data
public class RagDocumentDetail {
    private Integer id;
    private String fileName;
    private String ossUrl;
    private Integer uploadedBy;
    private String uploadedByName;
    private Integer ownerUserId;
    private String ownerUserName;
    private String knowledgeScope;
    private Integer chunkCount;
    private String extractedText;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}
