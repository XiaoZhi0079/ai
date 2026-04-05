package com.example.ai.service;

public interface RagPythonOcrService {

    String extractText(byte[] bytes, String fileName, String contentType);
}
