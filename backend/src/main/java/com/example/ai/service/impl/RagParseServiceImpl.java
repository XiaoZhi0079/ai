package com.example.ai.service.impl;

import com.example.ai.config.RagOcrProperties;
import com.example.ai.pojo.RagOcrRequestConfig;
import com.example.ai.pojo.RagParsePreview;
import com.example.ai.service.RagParseService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class RagParseServiceImpl implements RagParseService {

    private final RagOcrProperties ragOcrProperties;

    @Override
    public RagParsePreview parse(MultipartFile file, String knowledgeScope, RagOcrRequestConfig requestConfig) throws IOException {
        String fileName = file.getOriginalFilename();
        String extractedText;
        boolean ocrUsed;

        if (isPdf(fileName, file.getContentType())) {
            extractedText = extractPdfText(file, requestConfig);
            ocrUsed = true;
        } else if (isImage(file.getContentType(), fileName)) {
            extractedText = extractImageText(file, requestConfig);
            ocrUsed = true;
        } else {
            extractedText = extractStructuredText(file);
            ocrUsed = false;
        }

        RagParsePreview preview = new RagParsePreview();
        preview.setFileName(fileName);
        preview.setExtractedText(normalizeText(extractedText));
        preview.setOcrUsed(ocrUsed);
        preview.setCharCount(preview.getExtractedText() == null ? 0 : preview.getExtractedText().length());
        preview.setKnowledgeScope(knowledgeScope);
        return preview;
    }

    private String extractStructuredText(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName != null && (fileName.endsWith(".txt") || fileName.endsWith(".md"))) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        }
        TikaDocumentReader reader = new TikaDocumentReader(file.getResource());
        return joinDocuments(reader.get());
    }

    private String extractPdfText(MultipartFile file, RagOcrRequestConfig requestConfig) throws IOException {
        byte[] bytes = file.getBytes();
        try (PDDocument pdf = Loader.loadPDF(bytes)) {
            PDFRenderer renderer = new PDFRenderer(pdf);
            int pageCount = Math.min(pdf.getNumberOfPages(), ragOcrProperties.getMaxPages());
            return IntStream.range(0, pageCount)
                    .mapToObj(pageIndex -> renderAndOcrPage(renderer, pageIndex, requestConfig))
                    .collect(Collectors.joining("\n\n"));
        }
    }

    private String renderAndOcrPage(PDFRenderer renderer, int pageIndex, RagOcrRequestConfig requestConfig) {
        try {
            BufferedImage image = renderer.renderImageWithDPI(pageIndex, ragOcrProperties.getPdfDpi(), ImageType.RGB);
            byte[] pngBytes = toPngBytes(image);
            String pageText = callOcrModel(pngBytes, "page-" + (pageIndex + 1) + ".png", MimeTypeUtils.IMAGE_PNG, requestConfig);
            return "[Page %d]\n%s".formatted(pageIndex + 1, normalizeText(pageText));
        } catch (IOException ex) {
            throw new IllegalStateException("PDF OCR failed on page " + (pageIndex + 1), ex);
        }
    }

    private String extractImageText(MultipartFile file, RagOcrRequestConfig requestConfig) throws IOException {
        MimeType mimeType = resolveMimeType(file.getContentType(), file.getOriginalFilename());
        return callOcrModel(file.getBytes(), file.getOriginalFilename(), mimeType, requestConfig);
    }

    private String callOcrModel(byte[] bytes, String fileName, MimeType mimeType, RagOcrRequestConfig requestConfig) {
        RagOcrRequestConfig effectiveConfig = mergeConfig(requestConfig);

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(effectiveConfig.getBaseUrl())
                .apiKey(effectiveConfig.getApiKey())
                .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(effectiveConfig.getModel())
                        .temperature(0.0)
                        .maxTokens(ragOcrProperties.getMaxTokens())
                        .build())
                .build();

        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        UserMessage userMessage = UserMessage.builder()
                .text(ragOcrProperties.getPrompt())
                .media(new Media(mimeType, resource))
                .build();

        String content = ChatClient.create(chatModel)
                .prompt(new Prompt(List.of(userMessage)))
                .call()
                .content();
        return normalizeText(content);
    }

    private RagOcrRequestConfig mergeConfig(RagOcrRequestConfig requestConfig) {
        RagOcrRequestConfig effectiveConfig = new RagOcrRequestConfig();
        effectiveConfig.setBaseUrl(StringUtils.hasText(requestConfig == null ? null : requestConfig.getBaseUrl())
                ? requestConfig.getBaseUrl().trim()
                : ragOcrProperties.getBaseUrl());
        effectiveConfig.setApiKey(StringUtils.hasText(requestConfig == null ? null : requestConfig.getApiKey())
                ? requestConfig.getApiKey().trim()
                : ragOcrProperties.getApiKey());
        effectiveConfig.setModel(StringUtils.hasText(requestConfig == null ? null : requestConfig.getModel())
                ? requestConfig.getModel().trim()
                : ragOcrProperties.getModel());
        return effectiveConfig;
    }

    private String joinDocuments(List<Document> documents) {
        return documents.stream()
                .map(Document::getText)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n\n"));
    }

    private boolean isPdf(String fileName, String contentType) {
        return (fileName != null && fileName.toLowerCase().endsWith(".pdf"))
                || "application/pdf".equalsIgnoreCase(contentType);
    }

    private boolean isImage(String contentType, String fileName) {
        if (StringUtils.hasText(contentType) && contentType.toLowerCase().startsWith("image/")) {
            return true;
        }
        if (!StringUtils.hasText(fileName)) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".bmp") || lower.endsWith(".webp");
    }

    private MimeType resolveMimeType(String contentType, String fileName) {
        if (StringUtils.hasText(contentType)) {
            try {
                return MimeTypeUtils.parseMimeType(contentType);
            } catch (Exception ignored) {
            }
        }
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".png")) {
                return MimeTypeUtils.IMAGE_PNG;
            }
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                return MimeTypeUtils.parseMimeType("image/jpeg");
            }
            if (lower.endsWith(".webp")) {
                return MimeTypeUtils.parseMimeType("image/webp");
            }
        }
        return MimeTypeUtils.APPLICATION_OCTET_STREAM;
    }

    private byte[] toPngBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }

    private String normalizeText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.replace("\r\n", "\n").trim();
    }
}
