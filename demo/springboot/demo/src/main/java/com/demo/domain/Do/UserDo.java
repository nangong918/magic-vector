package com.demo.domain.Do;

import cn.hutool.core.util.IdUtil;
import lombok.Data;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.annotation.Id;

@Data
public class UserDo {
    @Id
    private Long id = IdUtil.getSnowflake().nextId();
    private String name;
    private String account;
    private String password;
    @Nullable
    private Long ossId = null;
}
