package com.example.ai.service;

import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;

import java.util.List;

public interface DocumentService {

    void loadText(Resource resource, String fileName, Integer docId, String knowledgeScope, Integer ownerUserId);

    void loadTextContent(String text, String fileName, Integer docId, String knowledgeScope, Integer ownerUserId);

    List<Document> doSearch(String question, Long userId);

    void deleteByDocumentId(Integer docId);
}
