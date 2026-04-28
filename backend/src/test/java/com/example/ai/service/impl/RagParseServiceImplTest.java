package com.example.ai.service.impl;

import com.example.ai.config.RagOcrProperties;
import com.example.ai.pojo.RagOcrRequestConfig;
import com.example.ai.pojo.RagParsePreview;
import com.example.ai.service.RagPythonOcrService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.MimeType;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagParseServiceImplTest {

    @Mock
    private RagPythonOcrService ragPythonOcrService;

    @Test
    void defaultImageOcrUsesPythonService() throws IOException {
        when(ragPythonOcrService.extractText(any(), eq("note.png"), eq("image/png"))).thenReturn("python text");

        RagParseServiceImpl service = new TestableRagParseServiceImpl(defaultProperties(), ragPythonOcrService);
        MockMultipartFile file = new MockMultipartFile("file", "note.png", "image/png", "img".getBytes());

        RagParsePreview preview = service.parse(file, "PRIVATE", new RagOcrRequestConfig());

        assertEquals("python text", preview.getOcrText());
        assertEquals("python text", preview.getExtractedText());
        assertTrue(preview.isOcrUsed());
        verify(ragPythonOcrService).extractText(any(), eq("note.png"), eq("image/png"));
    }

    @Test
    void customImageOcrUsesLegacyJavaFlow() throws IOException {
        RagParseServiceImpl service = new TestableRagParseServiceImpl(defaultProperties(), ragPythonOcrService);
        MockMultipartFile file = new MockMultipartFile("file", "note.png", "image/png", "img".getBytes());
        RagOcrRequestConfig config = new RagOcrRequestConfig();
        config.setBaseUrl("https://example.com");
        config.setApiKey("token");
        config.setModel("ocr-model");

        RagParsePreview preview = service.parse(file, "PRIVATE", config);

        assertEquals("legacy text", preview.getOcrText());
        assertEquals("legacy text", preview.getExtractedText());
        assertTrue(preview.isOcrUsed());
        verify(ragPythonOcrService, never()).extractText(any(), any(), any());
    }

    @Test
    void defaultPdfOcrUsesPythonServiceOnce() throws IOException {
        when(ragPythonOcrService.extractText(any(), eq("sample.pdf"), eq("application/pdf"))).thenReturn("pdf text");

        RagParseServiceImpl service = new TestableRagParseServiceImpl(defaultProperties(), ragPythonOcrService);
        MockMultipartFile file = new MockMultipartFile("file", "sample.pdf", "application/pdf", "%PDF".getBytes());

        RagParsePreview preview = service.parse(file, "PRIVATE", new RagOcrRequestConfig());

        assertEquals("pdf text", preview.getOcrText());
        assertEquals("pdf text", preview.getExtractedText());
        verify(ragPythonOcrService, times(1)).extractText(any(), eq("sample.pdf"), eq("application/pdf"));
    }

    private RagOcrProperties defaultProperties() {
        RagOcrProperties properties = new RagOcrProperties();
        properties.setMaxPages(2);
        properties.setPdfDpi(120);
        properties.setMaxEmbeddedImages(2);
        properties.setPrompt("OCR");
        return properties;
    }

    private static class TestableRagParseServiceImpl extends RagParseServiceImpl {
        TestableRagParseServiceImpl(RagOcrProperties ragOcrProperties, RagPythonOcrService ragPythonOcrService) {
            super(ragOcrProperties, ragPythonOcrService);
        }

        @Override
        protected String callLegacyOcrModel(byte[] bytes, String fileName, MimeType mimeType, RagOcrRequestConfig requestConfig) {
            return "legacy text";
        }
    }
}
