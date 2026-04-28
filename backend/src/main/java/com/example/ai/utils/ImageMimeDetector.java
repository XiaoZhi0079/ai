package com.example.ai.utils;

import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public final class ImageMimeDetector {

    private ImageMimeDetector() {}

    /**
     * 只暴露一个入口：传 MultipartFile 直接返回 MimeType
     * - 优先用文件头 magic number（最可靠）
     * - magic number 识别不到时，退回到 MultipartFile#getContentType
     * - 再识别不到则返回默认 IMAGE_JPEG（你也可以改成 APPLICATION_OCTET_STREAM）
     */
    public static MimeType detect(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return MimeTypeUtils.IMAGE_JPEG; // 或 MimeTypeUtils.APPLICATION_OCTET_STREAM
        }

        // 1) magic number sniff（最可靠）
        try (InputStream in = file.getInputStream()) {
            MimeType byMagic = detectByMagic(readHeader(in, 32));
            if (byMagic != null) return byMagic;
        } catch (IOException ignored) {
            // 继续走兜底
        }

        // 2) 兜底：浏览器传来的 Content-Type（可能为空/可能不准）
        String ct = file.getContentType();
        MimeType byContentType = normalizeContentType(ct);
        if (byContentType != null) return byContentType;

        // 3) 最后兜底
        return MimeTypeUtils.IMAGE_JPEG;
    }

    /** 读取固定长度的文件头字节（不会把整个文件读进内存） */
    private static byte[] readHeader(InputStream in, int maxBytes) throws IOException {
        byte[] buf = new byte[maxBytes];
        int n = 0;
        while (n < maxBytes) {
            int r = in.read(buf, n, maxBytes - n);
            if (r < 0) break;
            n += r;
        }
        if (n == maxBytes) return buf;

        byte[] trimmed = new byte[n];
        System.arraycopy(buf, 0, trimmed, 0, n);
        return trimmed;
    }

    /** 基于 magic number 判断格式：png/jpeg/gif/webp */
    private static MimeType detectByMagic(byte[] h) {
        if (h == null || h.length < 12) return null;

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (startsWith(h, new byte[]{(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})) {
            return MimeTypeUtils.IMAGE_PNG;
        }

        // JPEG: FF D8 FF
        if ((h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return MimeTypeUtils.IMAGE_JPEG;
        }

        // GIF: "GIF87a" or "GIF89a"
        if (h[0] == 'G' && h[1] == 'I' && h[2] == 'F'
                && h[3] == '8' && (h[4] == '7' || h[4] == '9') && h[5] == 'a') {
            return MimeTypeUtils.IMAGE_GIF;
        }

        // WEBP: "RIFF" .... "WEBP"
        if (h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h.length >= 12 && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
            return MimeType.valueOf("image/webp");
        }

        return null;
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    /** 规范化 Content-Type（去掉 ;charset=...），并转成 MimeType */
    private static MimeType normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) return null;
        String base = contentType.split(";", 2)[0].trim();

        return switch (base) {
            case MimeTypeUtils.IMAGE_PNG_VALUE  -> MimeTypeUtils.IMAGE_PNG;
            case MimeTypeUtils.IMAGE_JPEG_VALUE -> MimeTypeUtils.IMAGE_JPEG;
            case MimeTypeUtils.IMAGE_GIF_VALUE  -> MimeTypeUtils.IMAGE_GIF;
            default -> {
                try {
                    yield MimeType.valueOf(base); // 例如 image/webp
                } catch (Exception e) {
                    yield null;
                }
            }
        };
    }
}
