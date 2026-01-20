package com.example.ai.pojo;

import lombok.AllArgsConstructor;
import org.springframework.util.MimeType;
import lombok.Data;
import java.util.List;

@AllArgsConstructor
@Data
public class ImagesResponse {
    private String imageUrl; // 推荐：存 key
    private String previewUrl; // 可选：给前端预览用（短期或 CDN 公网）
    private MimeType mimeType;   // 可选：如果你想在 chat 时准确传 mime
}
