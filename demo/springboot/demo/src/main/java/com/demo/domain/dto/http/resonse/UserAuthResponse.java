package com.demo.domain.dto.http.resonse;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class UserAuthResponse {
    /** Wire as JSON string so JS clients keep full 64-bit precision; Java type stays Long. */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;
    private String account;
    private String name;
    private String avatarUrl;
    private String accessToken;
}
