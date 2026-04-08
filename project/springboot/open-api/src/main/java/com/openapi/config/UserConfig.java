package com.openapi.config;

import org.springframework.context.annotation.Configuration;

/**
 * @author 13225
 * @date 2025/10/14 15:49
 */
@Configuration
public class UserConfig {
    public String getBucketName() {
        return "user-bucket";
    }
}
