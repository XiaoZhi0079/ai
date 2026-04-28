package com.example.ai.service.impl;

import com.example.ai.pojo.ImagesResponse;
import com.example.ai.service.UploadService;
import com.example.ai.utils.AliyunOssClientPutObject;
import com.example.ai.utils.ImageMimeDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private final AliyunOssClientPutObject aliyunOssClientPutObject;

    public List<ImagesResponse> uploadImages(List<MultipartFile> imgFiles) throws Exception {

        if (imgFiles == null || imgFiles.isEmpty()) {
            return null;
        }
        List<ImagesResponse> imagesResponseslist = new ArrayList<>();
        for (MultipartFile file : imgFiles) {
            //上传图片
            String urlStr = aliyunOssClientPutObject.upload(file.getInputStream(), file.getOriginalFilename());
            String previewUrl = urlStr;
            MimeType mt = ImageMimeDetector.detect(file);
            String mime = mt == null ? null : mt.toString();
            ImagesResponse imagesResponse = new ImagesResponse(urlStr, previewUrl, mime);
            imagesResponseslist.add(imagesResponse);
        }
        return imagesResponseslist;
    }
}
