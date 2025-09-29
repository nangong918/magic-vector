package com.openapi.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

}
