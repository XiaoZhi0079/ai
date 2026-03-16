package com.example.ai.control;

import com.example.ai.mapper.RagDocumentMapper;
import com.example.ai.service.DocumentService;
import com.example.ai.pojo.LeeResult;
import com.example.ai.utils.AliyunOssClientPutObject;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rag")
public class RagController {

    @Resource
    private DocumentService documentService;

    @Resource
    private RagDocumentMapper ragDocumentMapper;

    @Resource
    private AliyunOssClientPutObject aliyunOssClientPutObject;

    @PostMapping("/upload")
    public LeeResult upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        String fileName = file.getOriginalFilename();

        // 1. 上传原件到 OSS
        String ossUrl;
        try {
            ossUrl = aliyunOssClientPutObject.upload(file.getInputStream(), fileName);
        } catch (Exception e) {
            return LeeResult.fail("文件上传OSS失败");
        }
        if (ossUrl == null) {
            return LeeResult.fail("文件上传OSS失败");
        }

        // 2. 元信息写入 MySQL
        Integer userId = null;
        String authUserId = (String) request.getAttribute("authUserId");
        if (authUserId != null) {
            userId = Integer.parseInt(authUserId);
        }
        ragDocumentMapper.insert(fileName, ossUrl, userId);

        // 3. 分块 + 向量化（现有逻辑）
        documentService.loadText(file.getResource(), fileName);

        return LeeResult.ok();
    }

    @GetMapping("/documents")
    public LeeResult<List<Map<String, Object>>> listDocuments() {
        return LeeResult.ok(ragDocumentMapper.selectAll());
    }

    @DeleteMapping("/documents/{id}")
    public LeeResult deleteDocument(@PathVariable Integer id) {
        ragDocumentMapper.deleteById(id);
        return LeeResult.ok();
    }
}
