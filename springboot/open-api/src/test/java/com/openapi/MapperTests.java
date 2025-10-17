package com.openapi;

import com.openapi.mapper.UserMapper;
import com.openapi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest(classes = MainApplication.class)
public class MapperTests {

    @Autowired
    public UserMapper userMapper;
    @Autowired
    public UserService userService;

    @Test
    public void userExistTest(){
        log.info("用户: {}, 是否存在：{}", "test_user", userMapper.selectById("test_user"));
        log.info("用户: {}, 是否存在：{}", "test_user", userService.checkUserExistById("test_user"));
    }
}
