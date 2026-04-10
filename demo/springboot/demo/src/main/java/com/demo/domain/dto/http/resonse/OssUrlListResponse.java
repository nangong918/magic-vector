package com.demo.domain.dto.http.resonse;

import lombok.Data;

import java.util.List;

@Data
public class OssUrlListResponse {
    private List<Long> fileIdList;
    private List<String> urlList;
}
