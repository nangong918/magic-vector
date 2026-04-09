package com.demo.converter;

import com.demo.domain.Do.UserDo;
import com.demo.domain.dto.http.resonse.UserAuthResponse;
import com.demo.domain.module.user.UserModule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserConverter {

    @Mapping(source = "id", target = "userId")
    @Mapping(source = "ossId", target = "avatarOssId")
    UserModule doToModule(UserDo userDo);

    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "accessToken", ignore = true)
    UserAuthResponse moduleToAuthResponse(UserModule userModule);

    default UserAuthResponse moduleToAuthResponse(
            UserModule userModule,
            String accessToken,
            String avatarUrl
    ) {
        UserAuthResponse response = moduleToAuthResponse(userModule);
        response.setAccessToken(accessToken);
        response.setAvatarUrl(avatarUrl);
        return response;
    }
}
