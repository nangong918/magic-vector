package com.openapi.domain.dto.http.resonse;

import lombok.Data;

@Data
public class UserPasswordUpdateResponse {
    private Long userId;
    private Boolean updated;
    private String message;
}
