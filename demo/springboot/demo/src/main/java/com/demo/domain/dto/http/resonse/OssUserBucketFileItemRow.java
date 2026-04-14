package com.demo.domain.dto.http.resonse;

import lombok.Data;

@Data
public class OssUserBucketFileItemRow {
    private String fileId;
    private String originFileName;
    private String url;
}
