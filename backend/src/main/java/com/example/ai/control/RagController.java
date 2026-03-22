package com.example.ai.control;

import com.example.ai.mapper.RagDocumentMapper;
import com.example.ai.pojo.LeeResult;
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
        RagParsePreview preview = ragParseService.parse(file, knowledgeScope, new RagOcrRequestConfig());
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
        ragDocumentMapper.insert(document);

        documentService.loadTextContent(text, fileName, document.getId(), knowledgeScope, ownerUserId);
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
        if (document == null || userId == null) {
            return false;
        }
        if (SCOPE_PUBLIC.equals(document.getKnowledgeScope())) {
            return "ADMIN".equals(role);
        }
        return userId.equals(document.getOwnerUserId());
    }
}
