package com.example.ai.service;

import com.example.ai.pojo.ImagesResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface UploadService {

    public List<ImagesResponse> uploadImages(@RequestParam("files") List<MultipartFile> imgFiles) throws Exception;
}
