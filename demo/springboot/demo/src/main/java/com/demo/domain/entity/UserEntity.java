package com.demo.domain.entity;

import cn.hutool.core.util.IdUtil;
import lombok.Data;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.annotation.Id;

@Data
public class UserEntity {
    @Id
    private Long id = IdUtil.getSnowflake().nextId();
    private String name;
    private String account;
    private String password;
    @Nullable
    private Long ossId = null;
}
