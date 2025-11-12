package com.openapi.config;

import com.openapi.domain.constant.ModelConstant;
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
                // 窗口大小设置为n
                .maxMessages(ModelConstant.MEMORY_CONTEXT_LENGTH)
                .build();
    }
}
