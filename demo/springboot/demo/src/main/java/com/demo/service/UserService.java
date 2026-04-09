package com.demo.service;

import com.demo.domain.module.user.UserModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    @Nullable Long createUser(
            @Nullable MultipartFile avatar,
            @NotNull String name,
            @NotNull String account,
            @NotNull String password);

    boolean checkUserExistById(Long id);

    boolean checkUserExistByAccount(String account);

    boolean checkPassword(@NotNull String account, @NotNull String password);

    boolean updatePasswordById(@NotNull Long userId, @NotNull String oldPassword, @NotNull String newPassword);

    @Nullable UserModule getUserModuleById(@NotNull Long id);

    @Nullable UserModule getUserModuleByAccount(@NotNull String account);
}
