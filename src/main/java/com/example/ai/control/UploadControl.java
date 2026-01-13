package com.example.ai.control;

import com.example.ai.pojo.ImagesResponse;
import com.example.ai.service.UploadService;
import com.example.ai.service.impl.ChatServiceImpl;
import com.example.ai.service.impl.UploadServiceImpl;
import com.example.ai.utils.AliyunOssClientPutObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.URL;
import java.util.List;


@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
@Slf4j
public class UploadControl {

    private final UploadService uploadService;

    @PostMapping("/images")
    public List<ImagesResponse> uploadImages(@RequestParam("files") List<MultipartFile> imgFiles) throws Exception {
        return uploadService.uploadImages(imgFiles);
    }


}
