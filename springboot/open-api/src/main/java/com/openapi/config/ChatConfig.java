package com.openapi.config;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Bean
    public MultiModalConversation getMultiModalConversation() {
        return new MultiModalConversation();
    }

}
