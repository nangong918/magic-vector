package com.openapi.service;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author 13225
 * @date 2025/10/14 15:44
 */
public interface UserService {
    @Nullable String createUser(
            @Nullable MultipartFile avatar,
            @NotNull String name,
            @NotNull String account,
            @NotNull String password);

    boolean checkUserExistById(String id);

    boolean checkUserExistByAccount(String account);

    boolean checkPassword(@NotNull String account, @NotNull String password);
}
