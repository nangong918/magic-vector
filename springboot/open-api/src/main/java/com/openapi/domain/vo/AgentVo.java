package com.openapi.domain.vo;

import lombok.Data;
import org.jetbrains.annotations.Nullable;

/**
 * @author 13225
 * @date 2025/9/29 13:46
 */
@Data
public class AgentVo {
    // agent name
    public String name;
    // agent description
    public String description;
    // agent avatarUrl
    @Nullable
    public String avatarUrl = null;
}
