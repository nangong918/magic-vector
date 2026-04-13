package com.demo.domain.dto.http.resonse;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class UserPasswordUpdateResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;
    private Boolean updated;
    private String message;
}
