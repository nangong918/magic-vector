package com.demo.mapper;

import com.demo.domain.Do.UserDo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    Integer insert(UserDo userDo);

    Integer deleteById(Long id);
    Integer deleteByAccount(String account);

    Integer updateNameById(
            @Param("id") Long id,
            @Param("name") String name
    );
    Integer updatePasswordById(
            @Param("id") Long id,
            @Param("password") String password
    );
    Integer updateAvatarById(
            @Param("id") Long id,
            @Param("ossId") Long ossId
    );

    UserDo selectById(Long id);
    UserDo selectByAccount(String account);
}
