package com.demo.domain.dto.http.resonse;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class UserTokenVerifyResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;
    private Boolean valid;
    private String message;
}
