package com.openapi.domain.module.user;

import lombok.Data;

/**
 * User 业务实体（Module），用于 Service 对外返回。
 * 不直接暴露数据库实体 UserDo，避免上层依赖持久化结构。
 */
@Data
public class UserModule {
    private Long userId;
    private String account;
    private String name;
    private Long avatarOssId;
}
