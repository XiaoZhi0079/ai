package com.example.ai.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RagDocumentInfo {

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
