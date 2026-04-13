package com.demo.domain.dto.http.resonse;

import lombok.Data;

import java.util.List;

@Data
public class OssBatchDeleteResponse {
    /** Each id as decimal string in JSON. */
    private List<String> fileIdList;
    private Integer successCount;
    private Integer failCount;
    private String message;
}
