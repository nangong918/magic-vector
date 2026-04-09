package com.demo.domain.module.user;

import lombok.Data;

@Data
public class UserModule {
    private Long userId;
    private String account;
    private String name;
    private Long avatarOssId;
}
