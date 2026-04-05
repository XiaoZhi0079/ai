package com.example.ai.service.impl;

import com.example.ai.config.RagOcrProperties;
import com.example.ai.pojo.RagOcrRequestConfig;
import com.example.ai.pojo.RagParsePreview;
import com.example.ai.service.RagParseService;
import com.example.ai.service.RagPythonOcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.hwpf.model.PicturesTable;
import org.apache.poi.hwpf.usermodel.Picture;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagParseServiceImpl implements RagParseService {

    private final RagOcrProperties ragOcrProperties;
    private final RagPythonOcrService ragPythonOcrService;

    @Override
    public RagParsePreview parse(MultipartFile file, String knowledgeScope, RagOcrRequestConfig requestConfig) throws IOException {
        String fileName = file.getOriginalFilename();
        StructuredExtractionResult extractionResult;

        if (isPdf(fileName, file.getContentType())) {
            extractionResult = new StructuredExtractionResult("", extractPdfText(file, requestConfig), true);
        } else if (isImage(file.getContentType(), fileName)) {
            extractionResult = new StructuredExtractionResult("", extractImageText(file, requestConfig), true);
        } else {
            extractionResult = extractStructuredText(file, requestConfig);
        }

        String extractedText = mergePreviewSections(extractionResult.structuredText(), extractionResult.ocrText());

        RagParsePreview preview = new RagParsePreview();
        preview.setFileName(fileName);
        preview.setExtractedText(normalizeText(extractedText));
        preview.setStructuredText(normalizeText(extractionResult.structuredText()));
        preview.setOcrText(normalizeText(extractionResult.ocrText()));
        preview.setOcrUsed(extractionResult.ocrUsed());
        preview.setCharCount(preview.getExtractedText() == null ? 0 : preview.getExtractedText().length());
        preview.setKnowledgeScope(knowledgeScope);
        return preview;
    }

    private StructuredExtractionResult extractStructuredText(MultipartFile file, RagOcrRequestConfig requestConfig) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName != null && (fileName.endsWith(".txt") || fileName.endsWith(".md"))) {
            return new StructuredExtractionResult(new String(file.getBytes(), StandardCharsets.UTF_8), "", false);
        }
        if (isDoc(fileName)) {
            return extractDocText(file, requestConfig);
        }
        if (isDocx(fileName)) {
            return extractDocxText(file, requestConfig);
        }
        TikaDocumentReader reader = new TikaDocumentReader(file.getResource());
        return new StructuredExtractionResult(joinDocuments(reader.get()), "", false);
    }

    private StructuredExtractionResult extractDocText(MultipartFile file, RagOcrRequestConfig requestConfig) throws IOException {
        byte[] bytes = file.getBytes();
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(bytes));
             WordExtractor extractor = new WordExtractor(document)) {
            String text = normalizeText(extractor.getText());
            PicturesTable picturesTable = document.getPicturesTable();
            String ocrText = picturesTable == null
                    ? ""
                    : extractEmbeddedDocImageText(picturesTable.getAllPictures(), requestConfig);
            return new StructuredExtractionResult(text, ocrText, StringUtils.hasText(ocrText));
        }
    }

    private StructuredExtractionResult extractDocxText(MultipartFile file, RagOcrRequestConfig requestConfig) throws IOException {
        byte[] bytes = file.getBytes();
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = normalizeText(extractor.getText());
            String ocrText = extractEmbeddedImageText(document.getAllPictures(), requestConfig);
            return new StructuredExtractionResult(text, ocrText, StringUtils.hasText(ocrText));
        }
    }

    private String extractEmbeddedImageText(List<XWPFPictureData> pictures, RagOcrRequestConfig requestConfig) {
        if (pictures == null || pictures.isEmpty()) {
            return "";
        }
        int maxImages = ragOcrProperties.getMaxEmbeddedImages() == null ? 0 : ragOcrProperties.getMaxEmbeddedImages();
        if (maxImages <= 0) {
            return "";
        }

        List<String> imageTexts = new ArrayList<>();
        int count = Math.min(pictures.size(), maxImages);
        for (int index = 0; index < count; index++) {
            XWPFPictureData picture = pictures.get(index);
            byte[] imageBytes = picture.getData();
            if (imageBytes == null || imageBytes.length == 0) {
                continue;
            }

            String fileName = picture.getFileName();
            if (!StringUtils.hasText(fileName)) {
                String ext = picture.suggestFileExtension();
                fileName = "embedded-image-" + (index + 1) + (StringUtils.hasText(ext) ? "." + ext : "");
            }
            String contentType = picture.getPackagePart() == null ? null : picture.getPackagePart().getContentType();
            MimeType mimeType = resolveMimeType(contentType, fileName);
            String ocrText = tryCallEmbeddedImageOcr(imageBytes, fileName, mimeType, requestConfig, index + 1);
            if (StringUtils.hasText(ocrText)) {
                imageTexts.add("[内嵌图片 %d]\n%s".formatted(index + 1, ocrText));
            }
        }
        return String.join("\n\n", imageTexts);
    }

    private String extractEmbeddedDocImageText(List<Picture> pictures, RagOcrRequestConfig requestConfig) {
        if (pictures == null || pictures.isEmpty()) {
            return "";
        }
        int maxImages = ragOcrProperties.getMaxEmbeddedImages() == null ? 0 : ragOcrProperties.getMaxEmbeddedImages();
        if (maxImages <= 0) {
            return "";
        }

        List<String> imageTexts = new ArrayList<>();
        int count = Math.min(pictures.size(), maxImages);
        for (int index = 0; index < count; index++) {
            Picture picture = pictures.get(index);
            byte[] imageBytes = picture.getContent();
            if (imageBytes == null || imageBytes.length == 0) {
                continue;
            }

            String fileName = picture.suggestFullFileName();
            if (!StringUtils.hasText(fileName)) {
                String ext = picture.suggestFileExtension();
                fileName = "embedded-image-" + (index + 1) + (StringUtils.hasText(ext) ? "." + ext : "");
            }
            MimeType mimeType = resolveMimeType(picture.getMimeType(), fileName);
            String ocrText = tryCallEmbeddedImageOcr(imageBytes, fileName, mimeType, requestConfig, index + 1);
            if (StringUtils.hasText(ocrText)) {
                imageTexts.add("[内嵌图片 %d]\n%s".formatted(index + 1, ocrText));
            }
        }
        return String.join("\n\n", imageTexts);
    }

    private String tryCallEmbeddedImageOcr(byte[] bytes,
                                           String fileName,
                                           MimeType mimeType,
                                           RagOcrRequestConfig requestConfig,
                                           int imageIndex) {
        try {
            return callConfiguredOcr(bytes, fileName, mimeType, requestConfig);
        } catch (Exception ex) {
            log.warn("Skip embedded image OCR: imageIndex={}, fileName={}", imageIndex, fileName, ex);
            return "";
        }
    }

    private String mergePreviewSections(String text, String ocrText) {
        List<String> sections = new ArrayList<>();
        if (StringUtils.hasText(text)) {
            sections.add(text);
        }
        if (StringUtils.hasText(ocrText)) {
            sections.add(StringUtils.hasText(text) ? "[图片 OCR 文本]\n" + ocrText : ocrText);
        }
        return String.join("\n\n", sections);
    }

    private String extractPdfText(MultipartFile file, RagOcrRequestConfig requestConfig) throws IOException {
        if (!shouldUseLegacyOcr(requestConfig)) {
            return callConfiguredOcr(
                    file.getBytes(),
                    file.getOriginalFilename(),
                    MimeTypeUtils.parseMimeType("application/pdf"),
                    requestConfig
            );
        }

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
            String pageText = callConfiguredOcr(pngBytes, "page-" + (pageIndex + 1) + ".png", MimeTypeUtils.IMAGE_PNG, requestConfig);
            return "[Page %d]\n%s".formatted(pageIndex + 1, normalizeText(pageText));
        } catch (IOException ex) {
            throw new IllegalStateException("PDF OCR failed on page " + (pageIndex + 1), ex);
        }
    }

    private String extractImageText(MultipartFile file, RagOcrRequestConfig requestConfig) throws IOException {
        MimeType mimeType = resolveMimeType(file.getContentType(), file.getOriginalFilename());
        return callConfiguredOcr(file.getBytes(), file.getOriginalFilename(), mimeType, requestConfig);
    }

    private String callConfiguredOcr(byte[] bytes, String fileName, MimeType mimeType, RagOcrRequestConfig requestConfig) {
        if (shouldUseLegacyOcr(requestConfig)) {
            return callLegacyOcrModel(bytes, fileName, mimeType, requestConfig);
        }
        return ragPythonOcrService.extractText(bytes, fileName, mimeType == null ? null : mimeType.toString());
    }

    protected String callLegacyOcrModel(byte[] bytes, String fileName, MimeType mimeType, RagOcrRequestConfig requestConfig) {
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

    private boolean shouldUseLegacyOcr(RagOcrRequestConfig requestConfig) {
        return StringUtils.hasText(requestConfig == null ? null : requestConfig.getBaseUrl())
                || StringUtils.hasText(requestConfig == null ? null : requestConfig.getApiKey())
                || StringUtils.hasText(requestConfig == null ? null : requestConfig.getModel());
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

    private boolean isDocx(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".docx");
    }

    private boolean isDoc(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".doc");
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

    private record StructuredExtractionResult(String structuredText, String ocrText, boolean ocrUsed) {
    }
}
