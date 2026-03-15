package com.example.ai.control;

import com.example.ai.service.DocumentService;
import com.example.ai.pojo.LeeResult;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/rag")
public class RagController {

    @Resource
    private DocumentService documentService;

    @PostMapping("/upload")
    public LeeResult upload(@RequestParam("file") MultipartFile file) {
        documentService.loadText(file.getResource(),file.getOriginalFilename());
        return LeeResult.ok();
    }

}
