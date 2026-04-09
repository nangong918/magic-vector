package com.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "demo.debug")
public class DebugConfig {
    private boolean enabled = false;
    private boolean tokenVerifyLogEnabled = false;

    public boolean shouldLogTokenVerify() {
        return enabled && tokenVerifyLogEnabled;
    }
}
