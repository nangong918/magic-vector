package com.openapi.config;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

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
        String jsonFilePath = "ai/visionPrompt.json";
        try (InputStream inputStream = getClass().getResourceAsStream(jsonFilePath)) {
            if (inputStream == null){
                log.warn("JSON文件不存在: {}", jsonFilePath);
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

    public String getVisionLLMPrompt(String visionResult) {
        // 读取JSON文件
        String jsonFilePath = "ai/visionLLMPrompt.json";
        try (InputStream inputStream = getClass().getResourceAsStream(jsonFilePath)) {
            if (inputStream == null){
                log.warn("JSON文件不存在: {}", jsonFilePath);
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                // 使用Gson解析JSON
                Gson gson = new Gson();
                String systemPrompt = Optional.ofNullable(gson.fromJson(reader, com.google.gson.JsonObject.class))
                        .map(it -> it.get("systemPrompt"))
                        .map(JsonElement::getAsString)
                        .orElse("");
                if (systemPrompt.isEmpty()) {
                    log.warn("visionLLMPrompt.json文件内容为空");
                    return systemPrompt + "visionAgent识别结果: <前端获取照片失败，无识别结果>";
                }
                else if (!StringUtils.hasText(visionResult)) {
                    log.warn("visionResult为空");
                } else {
                    return systemPrompt + "visionAgent识别结果: <" + visionResult + ">";
                }
            }
        } catch (Exception e) {
            log.error("JSON文件解析错误", e);
        }
        return null;
    }

}
