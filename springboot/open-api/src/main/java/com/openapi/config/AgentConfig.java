package com.openapi.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 13225
 * @date 2025/9/30 11:41
 */
@Configuration
public class AgentConfig {

    public String getBucketName() {
        return "agent-bucket";
    }

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
//                .chatMemoryRepository()
                // 窗口大小设置为10
                .maxMessages(10)
                .build();
    }
}
