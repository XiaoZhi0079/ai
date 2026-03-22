package com.example.ai.service.impl;

import com.example.ai.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private static final String SCOPE_PUBLIC = "PUBLIC";
    private static final String SCOPE_PRIVATE = "PRIVATE";

    private final RedisVectorStore redisVectorStore;

    @Override
    public void loadText(Resource resource, String fileName, Integer docId, String knowledgeScope, Integer ownerUserId) {
        TikaDocumentReader documentReader = new TikaDocumentReader(resource);
        List<Document> documents = documentReader.get();

        addDocuments(documents, fileName, docId, knowledgeScope, ownerUserId);
    }

    @Override
    public void loadTextContent(String text, String fileName, Integer docId, String knowledgeScope, Integer ownerUserId) {
        if (!StringUtils.hasText(text)) {
            return;
        }
        addDocuments(List.of(new Document(text)), fileName, docId, knowledgeScope, ownerUserId);
    }

    private void addDocuments(List<Document> documents, String fileName, Integer docId, String knowledgeScope, Integer ownerUserId) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
        List<Document> splitDocuments = tokenTextSplitter.apply(documents);

        List<Document> vectorDocuments = IntStream.range(0, splitDocuments.size())
                .mapToObj(index -> enrichChunk(splitDocuments.get(index), fileName, docId, knowledgeScope, ownerUserId, index))
                .toList();

        int batchSize = 8;
        for (int i = 0; i < vectorDocuments.size(); i += batchSize) {
            int end = Math.min(i + batchSize, vectorDocuments.size());
            redisVectorStore.add(vectorDocuments.subList(i, end));
        }
    }

    @Override
    public List<Document> doSearch(String question, Long userId) {
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(5)
                .similarityThresholdAll()
                .filterExpression(buildVisibilityFilter(userId))
                .build();
        return redisVectorStore.similaritySearch(request);
    }

    @Override
    public void deleteByDocumentId(Integer docId) {
        redisVectorStore.delete("where docId == " + docId);
    }

    private Document enrichChunk(Document document,
                                 String fileName,
                                 Integer docId,
                                 String knowledgeScope,
                                 Integer ownerUserId,
                                 int chunkIndex) {
        Map<String, Object> metadata = new HashMap<>(document.getMetadata());
        metadata.put("fileName", fileName);
        metadata.put("docId", docId);
        metadata.put("knowledgeScope", knowledgeScope);
        metadata.put("ownerUserId", ownerUserId == null ? -1 : ownerUserId);

        return Document.builder()
                .id("rag-doc-%d-chunk-%d".formatted(docId, chunkIndex))
                .text(document.getText())
                .metadata(metadata)
                .build();
    }

    private String buildVisibilityFilter(Long userId) {
        if (userId == null) {
            return "where knowledgeScope == 'PUBLIC'";
        }
        return "where knowledgeScope == '%s' || (knowledgeScope == '%s' && ownerUserId == %d)"
                .formatted(SCOPE_PUBLIC, SCOPE_PRIVATE, userId);
    }
}
