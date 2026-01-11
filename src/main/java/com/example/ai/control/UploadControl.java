//package com.example.ai.control;
//
//
//
//import com.example.ai.utils.AliyunOssClientPutObject;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.memory.ChatMemory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.util.MimeType;
//import org.springframework.util.MimeTypeUtils;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.net.MalformedURLException;
//import java.net.URL;
//
//@RestController
//public class UploadControl {
//
//
//
//    @Autowired
//    private AliyunOssClientPutObject aliyunOssClientPutObject;
//
//
//
//    @RequestMapping("/chat_image")
//    public String chatuploadimage(String userinput, String chatId, MultipartFile file) throws IOException {
//        String url = aliyunOssClientPutObject.upload(file.getInputStream(), file.getOriginalFilename());
//        return chatClient.prompt()
//                .user(u -> {
//                    try {
//                        u.text(userinput).media(MimeTypeUtils.IMAGE_PNG, new URL(url));
//                    } catch (MalformedURLException e) {
//                        throw new RuntimeException(e);
//                    }
//                })
//                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
//                .call()
//                .content();
//    }
//    @RequestMapping("/chat_audio")
//    public String chatuploadvedio(String userinput, String chatId, MultipartFile file) throws IOException {
//
//        MimeType audioMimeType = MimeType.valueOf("audio/mpeg");
//
//        // 传给 ChatClient
//        return chatClient.prompt()
//                .user(u -> {
//                        u.text(userinput).media(audioMimeType,file.getResource());
//                })
//                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
//                .call()
//                .content();
//    }
//
//}
