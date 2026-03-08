package com.openapi.service;

import com.openapi.domain.module.user.UserModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author 13225
 * @date 2025/10/14 15:44
 */
public interface UserService {
    @Nullable Long createUser(
            @Nullable MultipartFile avatar,
            @NotNull String name,
            @NotNull String account,
            @NotNull String password);

    boolean checkUserExistById(Long id);

    boolean checkUserExistByAccount(String account);

    boolean checkPassword(@NotNull String account, @NotNull String password);

    @Nullable UserModule getUserModuleById(@NotNull Long id);

    @Nullable UserModule getUserModuleByAccount(@NotNull String account);
}
