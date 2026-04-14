package com.demo.domain.dto.http.resonse;

import lombok.Data;

import java.util.List;

@Data
public class OssUserBucketListResponse {
    /** User id as decimal string on the wire. */
    private String userId;
    private List<String> bucketNameList;
}
