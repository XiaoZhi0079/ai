package com.example.ai;

import com.example.ai.config.ChatModelProperties;
import com.example.ai.service.ChatService;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;

import java.util.Map;


@Component
@SpringBootTest
class DemoApplicationTests {

@Autowired
 private ChatModelProperties chatModelProperties;

@Autowired
 private Map<String,Map<String, ChatClient>> chatModelConfig;

@Autowired
 private ChatService chatService;
//    @Test
//    void contextLoads() {
//        Map<String,Object> a= chatModelProperties.getPlatforms().stream().collect(
//                Collectors.toMap(
//                        ChatModelProperties.Platform::getName,
//                        platform -> platform.getOptions().stream().collect(
//                                Collectors.toMap(
//                                        ChatModelProperties.Platform.Options::getModel,
//                                        options -> options
//                                )
//                        )
//                )
//        );
//        for (Map.Entry<String, Object> entry : a.entrySet()) {
//            String key = entry.getKey();    // 获取键
//            Object value = entry.getValue();// 获取值
//            System.out.println("键：" + key + "，值：" + value);
//        }
//    }

    @Test
    void test1() {
        System.out.println(chatModelConfig);
    }

//    @Test
//    void test2() {
//         System.out.println(chatClientService.getChatClient().get("qwen3-coder").prompt()
//                    .user("你是谁")
//                    .call()
//                    .content());
//    }

    public class niming
    {
        public static void main(String[] args) {
            System.out.println("游泳比赛开始");
            Swim s1 = new Swim() {
                @Override
                public void swiming() {
                    System.out.println("老师开始游泳");
                }
            };
            competition(s1);
            System.out.println("------------------------------------");
            competition(new Swim() {
                @Override
                public void swiming() {
                    System.out.println("学生开始游泳");
                }
            });
        }

        static void competition(Swim s)
        {
            s.swiming();
        }
        interface Swim {
            void swiming();
        }
    }




}

