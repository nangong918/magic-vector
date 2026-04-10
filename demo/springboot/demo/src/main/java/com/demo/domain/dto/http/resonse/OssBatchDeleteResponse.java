package com.demo.domain.dto.http.resonse;

import lombok.Data;

import java.util.List;

@Data
public class OssBatchDeleteResponse {
    private List<Long> fileIdList;
    private Integer successCount;
    private Integer failCount;
    private String message;
}
