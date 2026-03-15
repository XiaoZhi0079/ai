package com.example.ai.utils;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.OSSClientBuilder;
import com.aliyun.sdk.service.oss2.credentials.CredentialsProvider;
import com.aliyun.sdk.service.oss2.credentials.StaticCredentialsProvider;
import com.aliyun.sdk.service.oss2.models.PutObjectRequest;
import com.aliyun.sdk.service.oss2.transport.BinaryData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;


@Slf4j
@RequiredArgsConstructor
@Component
public class AliyunOssClientPutObject {

    private final AliyunOSSProperties aliyunOSSProperties;

    public String upload(InputStream content, String originalFilename) {

        String endpoint = aliyunOSSProperties.getEndpoint();
        String bucket = aliyunOSSProperties.getBucket();
        String region = aliyunOSSProperties.getRegion();
        String accessKeyId = aliyunOSSProperties.getAccessKeyId();
        String accessKeySecret = aliyunOSSProperties.getAccessKeySecret();

        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String newFileName = UUID.randomUUID() + originalFilename.substring(originalFilename.lastIndexOf("."));
        String key = dir + "/" + newFileName;

        CredentialsProvider provider = new StaticCredentialsProvider(accessKeyId, accessKeySecret);
        OSSClientBuilder clientBuilder = OSSClient.newBuilder()
                .credentialsProvider(provider)
                .region(region);

        if (endpoint != null) {
            clientBuilder.endpoint(endpoint);
        }

        try (OSSClient client = clientBuilder.build()) {
            client.putObject(PutObjectRequest.newBuilder()
                    .bucket(bucket)
                    .key(key)
                    .body(BinaryData.fromStream(content))
                    .build());

            URL baseUrl = new URL(endpoint);
            return baseUrl.getProtocol() + "://" + bucket + "." + baseUrl.getHost() + "/" + key;
        } catch (Exception e) {
            log.error("OSS上传失败", e);
            return null;
        }
    }
}