package com.demo.controller;

import com.demo.domain.dto.BaseResponse;
import com.demo.domain.dto.req.UserTestReq;
import com.demo.domain.dto.resp.UserTestResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/test/network")
public class NetworkTestController {

    @GetMapping("/resetToken")
    public BaseResponse<UserTestResp> testResetUserToken(
            @RequestParam("account") String account
    ) {
        String newToken = String.valueOf(UUID.randomUUID());
        var userTestResp = new UserTestResp();
        userTestResp.setLoginToken(newToken);
        userTestResp.setAccount(account);
        return BaseResponse.getResponseEntitySuccess(userTestResp);
    }

    @PostMapping("/register")
    public BaseResponse<UserTestResp> testRegisterUser(
            @RequestBody UserTestReq req
    ) {
        var userTestResp = new UserTestResp();
        userTestResp.setAccount(req.getAccount());
        userTestResp.setLoginToken(String.valueOf(UUID.randomUUID()));
        return BaseResponse.getResponseEntitySuccess(userTestResp);
    }

}
