package com.demo.domain.dto.http.resonse;

import lombok.Data;

@Data
public class OssUserDeleteAllResponse {
    private Long userId;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private String message;
}
