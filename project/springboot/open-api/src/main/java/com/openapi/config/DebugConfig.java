package com.openapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "openapi.debug")
public class DebugConfig {
    /**
     * 总开关：是否启用调试日志
     */
    private boolean enabled = false;

    /**
     * token verify 接口日志开关
     */
    private boolean tokenVerifyLogEnabled = false;

    public boolean shouldLogTokenVerify() {
        return enabled && tokenVerifyLogEnabled;
    }
}
