package com.demo.domain.dto.http.resonse;

import lombok.Data;

import java.util.List;

@Data
public class OssUrlListResponse {
    /** Each id as decimal string in JSON (same convention as [Long] snowflakes). */
    private List<String> fileIdList;
    private List<String> urlList;
}
