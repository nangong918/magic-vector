package com.openapi.domain.Do;

import cn.hutool.core.util.IdUtil;
import lombok.Data;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.annotation.Id;


@Data
public class AgentDo {
    @Id
    private String id = String.valueOf(IdUtil.getSnowflake().nextId());
    private String name;
    private String description;
    @Nullable
    private String ossId;
}
