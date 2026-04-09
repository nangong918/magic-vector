package com.demo.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class UserConfig {
    public String getBucketName() {
        return "user-bucket";
    }
}
