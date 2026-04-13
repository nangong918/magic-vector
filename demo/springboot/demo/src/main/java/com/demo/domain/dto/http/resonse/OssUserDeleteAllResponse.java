package com.demo.domain.dto.http.resonse;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class OssUserDeleteAllResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private String message;
}
