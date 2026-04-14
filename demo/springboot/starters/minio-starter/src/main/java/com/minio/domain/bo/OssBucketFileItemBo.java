package com.minio.domain.bo;

import lombok.Data;

@Data
public class OssBucketFileItemBo {
    private Long fileId;
    private String originFileName;
    private String url;
}
