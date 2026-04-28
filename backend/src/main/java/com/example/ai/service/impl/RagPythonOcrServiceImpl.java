package com.example.ai.service.impl;

import com.example.ai.config.RagPythonOcrProperties;
import com.example.ai.pojo.PythonOcrRequest;
import com.example.ai.pojo.PythonOcrResponse;
import com.example.ai.service.RagPythonOcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RagPythonOcrServiceImpl implements RagPythonOcrService {

    private final RagPythonOcrProperties ragPythonOcrProperties;

    @Override
    public String extractText(byte[] bytes, String fileName, String contentType) {
        RestClient restClient = RestClient.builder()
                .baseUrl(ragPythonOcrProperties.getBaseUrl())
                .build();
        String safeFileName = buildSafeFileName(fileName, contentType);
        PythonOcrRequest body = new PythonOcrRequest();
        body.setFile(Base64.getEncoder().encodeToString(bytes));
        body.setFileName(safeFileName);
        body.setContentType(contentType == null ? "application/octet-stream" : contentType);

        try {
            PythonOcrResponse response = restClient.post()
                    .uri(ragPythonOcrProperties.getParsePath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(PythonOcrResponse.class);
            if (response == null) {
                throw new IllegalStateException("Python OCR service returned empty response");
            }
            if (StringUtils.hasText(response.getText())) {
                return response.getText().trim();
            }
            if (StringUtils.hasText(response.getMarkdown())) {
                return response.getMarkdown().trim();
            }
            return "";
        } catch (RestClientException ex) {
            throw new IllegalStateException("Python OCR request failed", ex);
        }
    }

    private String buildSafeFileName(String fileName, String contentType) {
        String extension = resolveExtension(fileName, contentType);
        if (!StringUtils.hasText(fileName)) {
            return "upload" + extension;
        }

        String normalized = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (!StringUtils.hasText(normalized)) {
            return "upload" + extension;
        }
        if (normalized.contains(".")) {
            return normalized;
        }
        return normalized + extension;
    }

    private String resolveExtension(String fileName, String contentType) {
        if (StringUtils.hasText(fileName) && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf('.'));
        }
        if (!StringUtils.hasText(contentType)) {
            return ".bin";
        }
        if (MimeTypeUtils.IMAGE_PNG_VALUE.equalsIgnoreCase(contentType)) {
            return ".png";
        }
        if ("image/jpeg".equalsIgnoreCase(contentType)) {
            return ".jpg";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return ".webp";
        }
        if ("image/bmp".equalsIgnoreCase(contentType)) {
            return ".bmp";
        }
        if ("application/pdf".equalsIgnoreCase(contentType)) {
            return ".pdf";
        }
        return ".bin";
    }
}
