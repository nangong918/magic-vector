package com.demo.domain.dto.http.resonse;

import lombok.Data;

import java.util.List;

@Data
public class OssUserBucketFileIdsResponse {
    private String userId;
    private String bucketName;
    /** File ids as decimal strings on the wire. */
    private List<String> fileIdList;
}
