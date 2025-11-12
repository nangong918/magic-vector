package com.openapi.mapper;

import com.openapi.domain.Do.UserDo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author 13225
 * @date 2025/10/14 15:36
 */
@Mapper
public interface UserMapper {
    /// 增
    Integer insert(UserDo userDo);

    /// 删
    Integer deleteById(String id);
    Integer deleteByAccount(String account);

    /// 改
    // 修改名字
    Integer updateNameById(
            @Param("id") String id,
            @Param("name") String name
    );
    // 修改密码
    Integer updatePasswordById(
            @Param("id") String id,
            @Param("password") String password
    );
    // 修改头像
    Integer updateAvatarById(
            @Param("id") String id,
            @Param("ossId") String ossId
    );

    /// 查
    UserDo selectById(String id);
    UserDo selectByAccount(String account);
}
