package com.openapi.config;

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

}
