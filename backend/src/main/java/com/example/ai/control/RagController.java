package com.example.ai.control;

import com.example.ai.mapper.RagDocumentMapper;
import com.example.ai.pojo.LeeResult;
import com.example.ai.pojo.RagDocumentDetail;
import com.example.ai.pojo.RagDocumentInfo;
import com.example.ai.pojo.RagOcrRequestConfig;
import com.example.ai.pojo.RagParsePreview;
import com.example.ai.service.DocumentService;
import com.example.ai.service.RagParseService;
import com.example.ai.service.RagOcrSettingsService;
import com.example.ai.utils.AliyunOssClientPutObject;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

@RestController
@RequestMapping("/rag")
public class RagController {

    private static final String SCOPE_PUBLIC = "PUBLIC";
    private static final String SCOPE_PRIVATE = "PRIVATE";

    @Resource
    private DocumentService documentService;

    @Resource
    private RagParseService ragParseService;

    @Resource
    private RagOcrSettingsService ragOcrSettingsService;

    @Resource
    private RagDocumentMapper ragDocumentMapper;

    @Resource
    private AliyunOssClientPutObject aliyunOssClientPutObject;

    @PostMapping("/parse")
    public LeeResult<RagParsePreview> parse(@RequestParam("file") MultipartFile file,
                                            @RequestParam(value = "scope", required = false) String scope,
                                            @RequestParam(value = "ocrBaseUrl", required = false) String ocrBaseUrl,
                                            @RequestParam(value = "ocrApiKey", required = false) String ocrApiKey,
                                            @RequestParam(value = "ocrModel", required = false) String ocrModel,
                                            HttpServletRequest request) throws IOException {
        Integer userId = currentUserId(request);
        if (userId == null) {
            return LeeResult.fail(401, "Missing user");
        }

        String knowledgeScope = resolveScope(scope, currentRole(request));
        RagParsePreview preview = ragParseService.parse(file, knowledgeScope,
                resolveOcrConfig(userId, ocrBaseUrl, ocrApiKey, ocrModel));
        return LeeResult.ok(preview);
    }

    @GetMapping("/ocr-settings")
    public LeeResult<RagOcrRequestConfig> getOcrSettings(HttpServletRequest request) {
        Integer userId = currentUserId(request);
        if (userId == null) {
            return LeeResult.fail(401, "Missing user");
        }
        return LeeResult.ok(ragOcrSettingsService.getUserSettings(userId));
    }

    @PostMapping("/ocr-settings")
    public LeeResult<RagOcrRequestConfig> saveOcrSettings(@RequestParam(value = "baseUrl", required = false) String baseUrl,
                                                          @RequestParam(value = "apiKey", required = false) String apiKey,
                                                          @RequestParam(value = "model", required = false) String model,
                                                          HttpServletRequest request) {
        Integer userId = currentUserId(request);
        if (userId == null) {
            return LeeResult.fail(401, "Missing user");
        }
        RagOcrRequestConfig config = new RagOcrRequestConfig();
        config.setBaseUrl(baseUrl);
        config.setApiKey(apiKey);
        config.setModel(model);
        return LeeResult.ok(ragOcrSettingsService.saveUserSettings(userId, config));
    }

    @PostMapping("/confirm")
    public LeeResult uploadConfirmed(@RequestParam("file") MultipartFile file,
                                     @RequestParam("text") String text,
                                     @RequestParam(value = "scope", required = false) String scope,
                                     HttpServletRequest request) {
        Integer userId = currentUserId(request);
        if (userId == null) {
            return LeeResult.fail(401, "Missing user");
        }
        if (!StringUtils.hasText(text)) {
            return LeeResult.fail("提取文本为空，请先检查 OCR 结果");
        }

        return saveConfirmedDocument(file, text, resolveScope(scope, currentRole(request)), userId);
    }

    @PostMapping("/upload")
    public LeeResult upload(@RequestParam("file") MultipartFile file,
                            @RequestParam(value = "scope", required = false) String scope,
                            HttpServletRequest request) throws IOException {
        Integer userId = currentUserId(request);
        if (userId == null) {
            return LeeResult.fail(401, "Missing user");
        }

        String knowledgeScope = resolveScope(scope, currentRole(request));
        RagParsePreview preview = ragParseService.parse(file, knowledgeScope,
                resolveOcrConfig(userId, null, null, null));
        if (!StringUtils.hasText(preview.getExtractedText())) {
            return LeeResult.fail("文档未提取到可用文本，请先使用预览确认流程");
        }
        return saveConfirmedDocument(file, preview.getExtractedText(), knowledgeScope, userId);
    }

    @GetMapping("/documents")
    public LeeResult<List<RagDocumentInfo>> listDocuments(HttpServletRequest request) {
        Integer userId = currentUserId(request);
        if (userId == null) {
            return LeeResult.fail(401, "Missing user");
        }
        return LeeResult.ok(ragDocumentMapper.selectVisibleDocuments(userId));
    }

    @GetMapping("/documents/{id}")
    public LeeResult<RagDocumentDetail> getDocument(@PathVariable Integer id, HttpServletRequest request) {
        RagDocumentDetail document = ragDocumentMapper.selectDetailById(id);
        if (document == null) {
            return LeeResult.fail("文档不存在");
        }

        Integer userId = currentUserId(request);
        if (userId == null || !canView(document.getKnowledgeScope(), document.getOwnerUserId(), userId)) {
            return LeeResult.fail(403, "无权查看该文档");
        }
        return LeeResult.ok(document);
    }

    @PostMapping("/documents/{id}/rename")
    public LeeResult renameDocument(@PathVariable Integer id,
                                    @RequestParam("fileName") String fileName,
                                    HttpServletRequest request) {
        RagDocumentInfo document = ragDocumentMapper.selectById(id);
        if (document == null) {
            return LeeResult.fail("文档不存在");
        }

        Integer userId = currentUserId(request);
        String role = currentRole(request);
        if (!canManage(document, userId, role)) {
            return LeeResult.fail(403, "无权修改该文档");
        }
        if (!StringUtils.hasText(fileName)) {
            return LeeResult.fail("文件名不能为空");
        }

        String normalizedFileName = normalizeRenamedFileName(document.getFileName(), fileName);
        ragDocumentMapper.updateFileName(id, normalizedFileName);
        return LeeResult.ok();
    }

    @PostMapping("/documents/{id}/re-ocr-preview")
    public LeeResult<RagParsePreview> reOcrPreview(@PathVariable Integer id,
                                                   HttpServletRequest request) throws IOException {
        RagDocumentInfo document = ragDocumentMapper.selectById(id);
        if (document == null) {
            return LeeResult.fail("文档不存在");
        }

        Integer userId = currentUserId(request);
        String role = currentRole(request);
        if (!canManage(document, userId, role)) {
            return LeeResult.fail(403, "无权重新解析该文档");
        }

        MultipartFile file = downloadMultipartFile(document);
        return LeeResult.ok(ragParseService.parse(file, document.getKnowledgeScope(),
                resolveOcrConfig(userId, null, null, null)));
    }

    @PostMapping("/documents/{id}/re-ocr-apply")
    public LeeResult<RagDocumentDetail> reOcrApply(@PathVariable Integer id,
                                                   @RequestParam("text") String text,
                                                   HttpServletRequest request) {
        RagDocumentInfo document = ragDocumentMapper.selectById(id);
        if (document == null) {
            return LeeResult.fail("文档不存在");
        }

        Integer userId = currentUserId(request);
        String role = currentRole(request);
        if (!canManage(document, userId, role)) {
            return LeeResult.fail(403, "无权覆盖该文档");
        }
        if (!StringUtils.hasText(text)) {
            return LeeResult.fail("提取文本不能为空");
        }

        documentService.deleteByDocumentId(id);
        int chunkCount = documentService.loadTextContent(text, document.getFileName(), id,
                document.getKnowledgeScope(), document.getOwnerUserId());
        ragDocumentMapper.updateContent(id, text, chunkCount);
        return LeeResult.ok(ragDocumentMapper.selectDetailById(id));
    }

    @DeleteMapping("/documents/{id}")
    public LeeResult deleteDocument(@PathVariable Integer id, HttpServletRequest request) {
        RagDocumentInfo document = ragDocumentMapper.selectById(id);
        if (document == null) {
            return LeeResult.fail("文档不存在");
        }

        Integer userId = currentUserId(request);
        String role = currentRole(request);
        if (!canDelete(document, userId, role)) {
            return LeeResult.fail(403, "无权删除该文档");
        }

        documentService.deleteByDocumentId(id);
        ragDocumentMapper.deleteById(id);
        return LeeResult.ok();
    }

    private LeeResult saveConfirmedDocument(MultipartFile file, String text, String knowledgeScope, Integer userId) {
        String fileName = file.getOriginalFilename();
        Integer ownerUserId = SCOPE_PRIVATE.equals(knowledgeScope) ? userId : null;

        String ossUrl;
        try {
            ossUrl = aliyunOssClientPutObject.upload(file.getInputStream(), fileName);
        } catch (Exception e) {
            return LeeResult.fail("文件上传 OSS 失败");
        }
        if (ossUrl == null) {
            return LeeResult.fail("文件上传 OSS 失败");
        }

        RagDocumentInfo document = new RagDocumentInfo();
        document.setFileName(fileName);
        document.setOssUrl(ossUrl);
        document.setUploadedBy(userId);
        document.setOwnerUserId(ownerUserId);
        document.setKnowledgeScope(knowledgeScope);
        document.setChunkCount(0);
        document.setExtractedText(text);
        ragDocumentMapper.insert(document);

        int chunkCount = documentService.loadTextContent(text, fileName, document.getId(), knowledgeScope, ownerUserId);
        ragDocumentMapper.updateContent(document.getId(), text, chunkCount);
        document.setChunkCount(chunkCount);
        return LeeResult.ok(document);
    }

    private RagOcrRequestConfig buildOcrConfig(String ocrBaseUrl, String ocrApiKey, String ocrModel) {
        RagOcrRequestConfig config = new RagOcrRequestConfig();
        config.setBaseUrl(ocrBaseUrl);
        config.setApiKey(ocrApiKey);
        config.setModel(ocrModel);
        return config;
    }

    private RagOcrRequestConfig resolveOcrConfig(Integer userId, String ocrBaseUrl, String ocrApiKey, String ocrModel) {
        if (StringUtils.hasText(ocrBaseUrl) || StringUtils.hasText(ocrApiKey) || StringUtils.hasText(ocrModel)) {
            return buildOcrConfig(ocrBaseUrl, ocrApiKey, ocrModel);
        }
        return ragOcrSettingsService.getUserSettings(userId);
    }

    private Integer currentUserId(HttpServletRequest request) {
        String authUserId = (String) request.getAttribute("authUserId");
        if (!StringUtils.hasText(authUserId)) {
            return null;
        }
        return Integer.parseInt(authUserId);
    }

    private String currentRole(HttpServletRequest request) {
        Object authRole = request.getAttribute("authRole");
        return authRole == null ? "" : String.valueOf(authRole).trim().toUpperCase();
    }

    private String resolveScope(String scope, String role) {
        String normalizedScope = StringUtils.hasText(scope) ? scope.trim().toUpperCase() : SCOPE_PRIVATE;
        if (SCOPE_PUBLIC.equals(normalizedScope) && "ADMIN".equals(role)) {
            return SCOPE_PUBLIC;
        }
        return SCOPE_PRIVATE;
    }

    private boolean canDelete(RagDocumentInfo document, Integer userId, String role) {
        return canManage(document, userId, role);
    }

    private boolean canManage(RagDocumentInfo document, Integer userId, String role) {
        if (document == null || userId == null) {
            return false;
        }
        if (SCOPE_PUBLIC.equals(document.getKnowledgeScope())) {
            return "ADMIN".equals(role);
        }
        return userId.equals(document.getOwnerUserId());
    }

    private boolean canView(String knowledgeScope, Integer ownerUserId, Integer userId) {
        if (SCOPE_PUBLIC.equals(knowledgeScope)) {
            return true;
        }
        return userId != null && userId.equals(ownerUserId);
    }

    private String normalizeRenamedFileName(String currentFileName, String newFileName) {
        String trimmed = newFileName.trim();
        int currentDot = currentFileName == null ? -1 : currentFileName.lastIndexOf('.');
        int newDot = trimmed.lastIndexOf('.');
        if (newDot > 0 || currentDot < 0) {
            return trimmed;
        }
        return trimmed + currentFileName.substring(currentDot);
    }

    private MultipartFile downloadMultipartFile(RagDocumentInfo document) throws IOException {
        URLConnection connection = new URL(document.getOssUrl()).openConnection();
        String contentType = connection.getContentType();
        byte[] bytes;
        try (InputStream inputStream = connection.getInputStream()) {
            bytes = inputStream.readAllBytes();
        }
        String fileName = document.getFileName();
        return new MultipartFile() {
            @Override
            public String getName() {
                return fileName;
            }

            @Override
            public String getOriginalFilename() {
                return fileName;
            }

            @Override
            public String getContentType() {
                return contentType;
            }

            @Override
            public boolean isEmpty() {
                return bytes.length == 0;
            }

            @Override
            public long getSize() {
                return bytes.length;
            }

            @Override
            public byte[] getBytes() {
                return bytes;
            }

            @Override
            public InputStream getInputStream() {
                return new java.io.ByteArrayInputStream(bytes);
            }

            @Override
            public void transferTo(java.io.File dest) throws IOException {
                java.nio.file.Files.write(dest.toPath(), bytes);
            }
        };
    }
}
