package com.example.ai.service.impl;


import com.example.ai.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final RedisVectorStore redisVectorStore;

    @Override
    public void loadText(Resource resource, String fileName) {
        // 加载读取文档
        TextReader textReader = new TextReader(resource);
        textReader.getCustomMetadata().put("fileName", fileName);
        List<Document> documents = textReader.get();
        
        // 切割文档：使用 TokenTextSplitter (默认约800 token) 替代按段落切割
        // 这样可以避免标题单独成段，且保留上下文
        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
        List<Document> list = tokenTextSplitter.apply(documents);
        
        // 向量存储 - 手动分批处理，避免API Batch Size限制 (如DashScope限制10-25条)
        int batchSize = 8;
        for (int i = 0; i < list.size(); i += batchSize) {
            int end = Math.min(i + batchSize, list.size());
            redisVectorStore.add(list.subList(i, end));
        }
    }

    @Override
    public List<Document> doSearch(String question) {
        return redisVectorStore.similaritySearch(question);
    }

}

