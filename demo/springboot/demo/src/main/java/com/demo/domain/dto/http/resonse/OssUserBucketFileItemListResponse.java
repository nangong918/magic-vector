package com.demo.domain.dto.http.resonse;

import lombok.Data;

import java.util.List;

@Data
public class OssUserBucketFileItemListResponse {
    private String userId;
    private String bucketName;
    private List<OssUserBucketFileItemRow> items;
}
