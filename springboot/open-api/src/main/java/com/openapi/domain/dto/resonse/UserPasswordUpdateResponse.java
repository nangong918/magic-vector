package com.openapi.domain.dto.resonse;

import lombok.Data;

@Data
public class UserPasswordUpdateResponse {
    private Long userId;
    private Boolean updated;
    private String message;
}
