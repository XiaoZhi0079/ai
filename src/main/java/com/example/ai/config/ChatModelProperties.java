package com.example.ai.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;


@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class ChatModelProperties {


    private List<ChatModelProperties.Platform> platforms;


    @Data
    public static class Platform {
        private String name;
        private String apiKey;
        private String baseUrl;
        private List<ChatModelProperties.Platform.Options> options;

        @Data
        public static class Options {
            private String model;
            private Double temperature;
            private Integer maxTokens;

        }
    }
}