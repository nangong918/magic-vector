package com.openapi.config;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class ChatConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    @Bean
    public ChatClient getChatClient(@Autowired DashScopeChatModel dashScopeChatModel) {
        return ChatClient.builder(dashScopeChatModel)
//                .defaultOptions(chatOptions)
                .build();
    }

    @Bean
    public Recognition getRecognition() {
        return new Recognition();
    }

    // 此方法是TTS和VL模型的公用模型：多模态模型
    @Bean
    public MultiModalConversation getMultiModalConversation() {
        return new MultiModalConversation();
    }

    @Bean("visionPrompt")
    public com.google.gson.JsonObject getVisionPrompt() {
        // 读取JSON文件
        try (InputStream inputStream = getClass().getResourceAsStream("/ai/visionPrompt.json")) {
            if (inputStream == null){
                log.warn("JSON文件不存在");
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                // 使用Gson解析JSON
                Gson gson = new Gson();

                return gson.fromJson(reader, com.google.gson.JsonObject.class);
            }
        } catch (Exception e) {
            log.error("JSON文件解析错误", e);
        }
        return null;
    }

}
