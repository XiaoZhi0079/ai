package com.example.ai.utils;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.OSSClientBuilder;
import com.aliyun.sdk.service.oss2.credentials.CredentialsProvider;
import com.aliyun.sdk.service.oss2.credentials.EnvironmentVariableCredentialsProvider;
import com.aliyun.sdk.service.oss2.models.PutObjectRequest;
import com.aliyun.sdk.service.oss2.models.PutObjectResult;
import com.aliyun.sdk.service.oss2.transport.BinaryData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;



@Component
public class AliyunOssClientPutObject {

@Autowired
private AliyunOSSProperties aliyunOSSProperties;
//
//    @Value("${aliyun.oss.endpoint}")
//    String endpoint;
//    @Value("${aliyun.oss.region}")
//    String region;
//    @Value("${aliyun.oss.bucket}")
//    String bucket;


    public String upload(InputStream content, String originalFilename) {


        String endpoint=aliyunOSSProperties.getEndpoint();
        String region=aliyunOSSProperties.getRegion();
        String bucket=aliyunOSSProperties.getBucket();

        //获取当前系统日期的字符串,格式为 yyyy/MM
        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        //生成一个新的不重复的文件名
        String newFileName = UUID.randomUUID() + originalFilename.substring(originalFilename.lastIndexOf("."));
        String key = dir + "/" + newFileName;


        CredentialsProvider provider = new EnvironmentVariableCredentialsProvider();
        OSSClientBuilder clientBuilder = OSSClient.newBuilder()
                .credentialsProvider(provider)
                .region(region);

        if (endpoint != null) {
            clientBuilder.endpoint(endpoint);
        }

        try (OSSClient client = clientBuilder.build()) {


            PutObjectResult result = client.putObject(PutObjectRequest.newBuilder()
                    .bucket(bucket)
                    .key(key)
                    .body(BinaryData.fromStream(content))
                    .build());
//            System.out.printf("status code:%d, request id:%s, eTag:%s\n",
//                    result.statusCode(), result.requestId(), result.eTag());

            URL baseUrl = new URL(endpoint);
            String url = baseUrl.getProtocol() + "://" + bucket + "." + baseUrl.getHost() + "/" + key;
            return url;

        } catch (Exception e) {
            //If the exception is caused by ServiceException, detailed information can be obtained in this way.
            // ServiceException se = ServiceException.asCause(e);
            // if (se != null) {
            //    System.out.printf("ServiceException: requestId:%s, errorCode:%s\n", se.requestId(), se.errorCode());
            //}
            System.out.printf("error:\n%s", e);
            return null;
        }
    }


}