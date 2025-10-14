package com.openapi.domain.Do;

import cn.hutool.core.util.IdUtil;
import lombok.Data;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.annotation.Id;

/**
 * @author 13225
 * @date 2025/10/14 15:29
 * table: user
 */
@Data
public class UserDo {
    @Id
    // 后端搜索的id
    private String id = String.valueOf(IdUtil.getSnowflake().nextId());
    // user自己可以随便改变的name
    private String name;
    // user之间搜索的account
    private String account;
    private String password;
    @Nullable
    private String ossId;
}
