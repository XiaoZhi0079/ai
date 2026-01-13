package com.example.ai.service.impl;

import com.example.ai.pojo.ImagesResponse;
import com.example.ai.service.UploadService;
import com.example.ai.utils.AliyunOssClientPutObject;
import com.example.ai.utils.ImageMimeDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private final AliyunOssClientPutObject aliyunOssClientPutObject;

    public List<ImagesResponse> uploadImages(@RequestParam("files") List<MultipartFile> imgFiles) throws Exception {
        if (imgFiles == null || imgFiles.isEmpty()) {
            return null;
        }
        List<ImagesResponse> imagesResponseslist = new ArrayList<>();
        for (MultipartFile file : imgFiles) {
            //上传图片
            String urlStr = aliyunOssClientPutObject.upload(file.getInputStream(), file.getOriginalFilename());
            String pre = "预览";
            MimeType mt = ImageMimeDetector.detect(file);
            ImagesResponse imagesResponse = new ImagesResponse(urlStr,pre,mt);
            imagesResponseslist.add(imagesResponse);
        }
        return imagesResponseslist;
    }
}
