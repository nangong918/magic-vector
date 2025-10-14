package com.openapi.service.impl;

import com.minio.domain.ao.SuccessFile;
import com.minio.service.OssService;
import com.openapi.config.UserConfig;
import com.openapi.domain.Do.UserDo;
import com.openapi.mapper.UserMapper;
import com.openapi.service.UserServiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * @author 13225
 * @date 2025/10/14 15:44
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceServiceImpl implements UserServiceService {

    private final OssService ossService;
    private final UserMapper userMapper;
    private final UserConfig userConfig;

    /**
     * 创建用户
     * @param avatar    头像
     * @param name      名字
     * @param account   账号
     * @param password  密码
     * @return          用户id
     */
    @Nullable
    @Override
    public String createUser(
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
            val result = ossService.uploadFiles(
                    files,
                    userDo.getId(),
                    userConfig.getBucketName()
            );
            String ossId = Optional.ofNullable(result.getSuccessFiles())
                    .filter(list -> !list.isEmpty())
                    .map(List::getFirst)
                    .map(SuccessFile::getFileId)
                    .orElse(null);
            userDo.setOssId(ossId);
            userMapper.insert(userDo);
        }
        if (userMapper.insert(userDo) > 0){
            return userDo.getId();
        }
        else {
            return null;
        }
    }

    @Override
    public boolean checkUserExist(String account){
        return userMapper.selectByAccount(account) != null;
    }

    /**
     * 检查账密
     * @param account   账号
     * @param password  密码
     * @return          账号密码是否正确
     */
    @Override
    public boolean checkPassword(@NotNull String account, @NotNull String password){
        UserDo userDo = userMapper.selectByAccount(account);
        if (userDo == null || userDo.getId() == null){
            return false;
        }
        return password.equals(userDo.getPassword());
    }
}
