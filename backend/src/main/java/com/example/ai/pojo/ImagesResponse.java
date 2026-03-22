package com.example.ai.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ImagesResponse {
    private String imageUrl; // 推荐：存 key
    private String previewUrl; // 可选：给前端预览用（短期或 CDN 公网）
    private String mimeType;   // 可选：MIME type 字符串
}
