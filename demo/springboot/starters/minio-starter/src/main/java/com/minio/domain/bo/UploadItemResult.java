package com.minio.domain.bo;

import lombok.Data;

@Data
public class UploadItemResult {
    private String originFileName;
    private boolean success;
    private boolean duplicated;
    private Long fileId;
    private String url;
    private String message;
}
