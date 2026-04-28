package com.example.ai.service;

import com.example.ai.pojo.RagOcrRequestConfig;
import com.example.ai.pojo.RagParsePreview;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface RagParseService {

    RagParsePreview parse(MultipartFile file, String knowledgeScope, RagOcrRequestConfig requestConfig) throws IOException;
}
