package com.example.ai.service.impl;

import com.example.ai.mapper.RagDocumentMapper;
import com.example.ai.pojo.RagDocumentInfo;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private static final String SCOPE_PUBLIC = "PUBLIC";
    private static final String SCOPE_PRIVATE = "PRIVATE";
    private static final int VECTOR_SEARCH_TOP_K = 10;
    private static final int FINAL_SEARCH_TOP_K = 5;
    private static final int KEYWORD_SEARCH_TOP_K = 3;
    private static final int EXCERPT_RADIUS = 180;
    private static final int MAX_SEARCH_TERMS = 24;
    private static final Pattern CJK_SEGMENT_PATTERN = Pattern.compile("[\\u4e00-\\u9fff]{2,}");

    private final RedisVectorStore redisVectorStore;
    private final RagDocumentMapper ragDocumentMapper;

    @Override
    public int loadText(Resource resource, String fileName, Integer docId, String knowledgeScope, Integer ownerUserId) {
        TikaDocumentReader documentReader = new TikaDocumentReader(resource);
        List<Document> documents = documentReader.get();

        return addDocuments(documents, fileName, docId, knowledgeScope, ownerUserId);
    }

    @Override
    public int loadTextContent(String text, String fileName, Integer docId, String knowledgeScope, Integer ownerUserId) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        return addDocuments(List.of(new Document(text)), fileName, docId, knowledgeScope, ownerUserId);
    }

    private int addDocuments(List<Document> documents, String fileName, Integer docId, String knowledgeScope, Integer ownerUserId) {
        if (documents == null || documents.isEmpty()) {
            return 0;
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

        return vectorDocuments.size();
    }

    @Override
    public List<Document> doSearch(String question, Long userId) {
        if (!StringUtils.hasText(question)) {
            return List.of();
        }

        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(VECTOR_SEARCH_TOP_K)
                .similarityThresholdAll()
                .filterExpression(buildVisibilityFilter(userId))
                .build();

        List<Document> vectorMatches = redisVectorStore.similaritySearch(request);
        List<Document> keywordMatches = keywordSearch(question, userId);
        List<Document> rerankedVectorMatches = rerankVectorMatches(vectorMatches, question);

        List<Document> merged = new ArrayList<>(FINAL_SEARCH_TOP_K);
        Set<String> seenKeys = new LinkedHashSet<>();
        appendDistinct(merged, seenKeys, keywordMatches);
        appendDistinct(merged, seenKeys, rerankedVectorMatches);
        return merged;
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
        metadata.put("chunkIndex", chunkIndex);

        return Document.builder()
                .id("rag-doc-%d-chunk-%d".formatted(docId, chunkIndex))
                .text(buildIndexedText(fileName, document.getText()))
                .metadata(metadata)
                .build();
    }

    private void appendDistinct(List<Document> target, Set<String> seenKeys, List<Document> candidates) {
        for (Document candidate : candidates) {
            String key = buildDedupKey(candidate);
            if (seenKeys.add(key)) {
                target.add(candidate);
            }
            if (target.size() >= FINAL_SEARCH_TOP_K) {
                return;
            }
        }
    }

    private List<Document> keywordSearch(String question, Long userId) {
        Integer searchUserId = userId == null ? null : Math.toIntExact(userId);
        List<RagDocumentInfo> candidates = ragDocumentMapper.selectVisibleDocumentsForSearch(searchUserId);
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        return IntStream.range(0, candidates.size())
                .mapToObj(index -> buildKeywordCandidate(candidates.get(index), question, index))
                .filter(candidate -> candidate != null && candidate.score() > 0)
                .sorted(Comparator.comparingInt(ScoredDocument::score).reversed()
                        .thenComparingInt(ScoredDocument::index))
                .limit(KEYWORD_SEARCH_TOP_K)
                .map(ScoredDocument::document)
                .toList();
    }

    private ScoredDocument buildKeywordCandidate(RagDocumentInfo document, String question, int index) {
        String corpus = buildSearchCorpus(document.getFileName(), document.getExtractedText());
        int score = scoreTextMatch(question, corpus);
        if (score <= 0) {
            return null;
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileName", document.getFileName());
        metadata.put("docId", document.getId());
        metadata.put("knowledgeScope", document.getKnowledgeScope());
        metadata.put("ownerUserId", document.getOwnerUserId() == null ? -1 : document.getOwnerUserId());

        Document keywordDocument = Document.builder()
                .id("rag-keyword-%d".formatted(document.getId()))
                .text(buildExcerpt(corpus, question))
                .metadata(metadata)
                .build();
        return new ScoredDocument(keywordDocument, score, index);
    }

    private List<Document> rerankVectorMatches(List<Document> vectorMatches, String question) {
        if (vectorMatches == null || vectorMatches.isEmpty()) {
            return List.of();
        }

        return IntStream.range(0, vectorMatches.size())
                .mapToObj(index -> {
                    Document document = vectorMatches.get(index);
                    String fileName = metadataValue(document, "fileName");
                    int score = scoreTextMatch(question, buildSearchCorpus(fileName, document.getText()));
                    return new ScoredDocument(document, score, index);
                })
                .sorted(Comparator.comparingInt(ScoredDocument::score).reversed()
                        .thenComparingInt(ScoredDocument::index))
                .map(ScoredDocument::document)
                .toList();
    }

    private String buildIndexedText(String fileName, String text) {
        if (!StringUtils.hasText(fileName)) {
            return text;
        }
        return "文件名：" + fileName.trim() + "\n" + (text == null ? "" : text);
    }

    private String buildSearchCorpus(String fileName, String text) {
        if (!StringUtils.hasText(fileName)) {
            return text == null ? "" : text;
        }
        return fileName.trim() + "\n" + (text == null ? "" : text);
    }

    private String buildExcerpt(String text, String question) {
        if (!StringUtils.hasText(text)) {
            return "";
        }

        String lowerText = text.toLowerCase(Locale.ROOT);
        int matchIndex = -1;
        for (String term : extractSearchTerms(question)) {
            if (!StringUtils.hasText(term)) {
                continue;
            }
            matchIndex = lowerText.indexOf(term.toLowerCase(Locale.ROOT));
            if (matchIndex >= 0) {
                break;
            }
        }

        if (matchIndex < 0) {
            return abbreviate(text, EXCERPT_RADIUS * 2);
        }

        int start = Math.max(0, matchIndex - EXCERPT_RADIUS);
        int end = Math.min(text.length(), matchIndex + EXCERPT_RADIUS);
        String excerpt = text.substring(start, end).trim();
        if (start > 0) {
            excerpt = "..." + excerpt;
        }
        if (end < text.length()) {
            excerpt = excerpt + "...";
        }
        return excerpt;
    }

    private String abbreviate(String text, int maxLength) {
        if (!StringUtils.hasText(text) || text.length() <= maxLength) {
            return text == null ? "" : text;
        }
        return text.substring(0, maxLength).trim() + "...";
    }

    private int scoreTextMatch(String question, String text) {
        String normalizedQuestion = normalizeSearchText(question);
        String normalizedText = normalizeSearchText(text);
        if (!StringUtils.hasText(normalizedQuestion) || !StringUtils.hasText(normalizedText)) {
            return 0;
        }

        int score = normalizedText.contains(normalizedQuestion) ? 120 : 0;
        for (String term : extractSearchTerms(normalizedQuestion)) {
            if (term.length() < 2) {
                continue;
            }
            if (normalizedText.contains(term)) {
                score += 18 + Math.min(term.length(), 8);
            }
        }
        return score;
    }

    private List<String> extractSearchTerms(String question) {
        String normalizedQuestion = normalizeSearchText(question);
        if (!StringUtils.hasText(normalizedQuestion)) {
            return List.of();
        }

        LinkedHashSet<String> terms = new LinkedHashSet<>();
        if (normalizedQuestion.length() <= 32) {
            terms.add(normalizedQuestion);
        }

        for (String token : normalizedQuestion.split("[\\p{Punct}\\s]+")) {
            if (token.length() >= 2) {
                terms.add(token);
            }
            if (terms.size() >= MAX_SEARCH_TERMS) {
                return new ArrayList<>(terms);
            }
        }

        Matcher matcher = CJK_SEGMENT_PATTERN.matcher(normalizedQuestion);
        while (matcher.find() && terms.size() < MAX_SEARCH_TERMS) {
            addCjkNgrams(terms, matcher.group());
        }
        return new ArrayList<>(terms);
    }

    private void addCjkNgrams(Set<String> terms, String segment) {
        int maxGram = Math.min(4, segment.length());
        for (int length = maxGram; length >= 2 && terms.size() < MAX_SEARCH_TERMS; length--) {
            for (int index = 0; index <= segment.length() - length && terms.size() < MAX_SEARCH_TERMS; index++) {
                terms.add(segment.substring(index, index + length));
            }
        }
    }

    private String normalizeSearchText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private String metadataValue(Document document, String key) {
        Object value = document.getMetadata() == null ? null : document.getMetadata().get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private String buildDedupKey(Document document) {
        Object docId = document.getMetadata() == null ? null : document.getMetadata().get("docId");
        String logicalId = docId == null ? document.getId() : String.valueOf(docId);
        return logicalId + "|" + normalizeSearchText(document.getText());
    }

    private String buildVisibilityFilter(Long userId) {
        if (userId == null) {
            return "where knowledgeScope == 'PUBLIC'";
        }
        return "where knowledgeScope == '%s' || (knowledgeScope == '%s' && ownerUserId == %d)"
                .formatted(SCOPE_PUBLIC, SCOPE_PRIVATE, userId);
    }

    private record ScoredDocument(Document document, int score, int index) {
    }
}
