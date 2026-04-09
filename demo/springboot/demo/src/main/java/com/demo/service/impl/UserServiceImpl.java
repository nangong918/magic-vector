package com.demo.service.impl;

import com.demo.config.UserConfig;
import com.demo.converter.UserConverter;
import com.demo.domain.Do.UserDo;
import com.demo.domain.module.user.UserModule;
import com.demo.mapper.UserMapper;
import com.demo.service.UserService;
import com.minio.domain.dto.BatchUploadResult;
import com.minio.domain.dto.UploadItemResult;
import com.minio.service.OssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final OssService ossService;
    private final UserMapper userMapper;
    private final UserConfig userConfig;
    private final UserConverter userConverter;

    @Nullable
    @Override
    public Long createUser(
            @Nullable MultipartFile avatar,
            @NotNull String name,
            @NotNull String account,
            @NotNull String password){
        UserDo userDo = new UserDo();
        userDo.setName(name);
        userDo.setAccount(account);
        userDo.setPassword(password);
        if (avatar != null) {
            val files = List.of(avatar);
            BatchUploadResult result = ossService.uploadFiles(
                    files,
                    userDo.getId(),
                    userConfig.getBucketName()
            );
            Long ossId = Optional.ofNullable(result.getItems())
                    .filter(list -> !list.isEmpty())
                    .map(List::getFirst)
                    .filter(UploadItemResult::isSuccess)
                    .map(UploadItemResult::getFileId)
                    .orElse(null);
            userDo.setOssId(ossId);
        }
        if (userMapper.insert(userDo) > 0){
            return userDo.getId();
        }
        else {
            return null;
        }
    }

    @Override
    public boolean checkUserExistById(Long id){
        return userMapper.selectById(id) != null;
    }

    @Override
    public boolean checkUserExistByAccount(String account){
        return userMapper.selectByAccount(account) != null;
    }

    @Override
    public boolean checkPassword(@NotNull String account, @NotNull String password){
        UserDo userDo = userMapper.selectByAccount(account);
        if (userDo == null || userDo.getId() == null){
            return false;
        }
        return password.equals(userDo.getPassword());
    }

    @Override
    public boolean updatePasswordById(@NotNull Long userId, @NotNull String oldPassword, @NotNull String newPassword) {
        UserDo userDo = userMapper.selectById(userId);
        if (userDo == null || userDo.getId() == null) {
            return false;
        }
        if (!oldPassword.equals(userDo.getPassword())) {
            return false;
        }
        return userMapper.updatePasswordById(userId, newPassword) > 0;
    }

    @Nullable
    @Override
    public UserModule getUserModuleById(@NotNull Long id) {
        UserDo userDo = userMapper.selectById(id);
        return userDo == null ? null : userConverter.doToModule(userDo);
    }

    @Nullable
    @Override
    public UserModule getUserModuleByAccount(@NotNull String account) {
        UserDo userDo = userMapper.selectByAccount(account);
        return userDo == null ? null : userConverter.doToModule(userDo);
    }
}
