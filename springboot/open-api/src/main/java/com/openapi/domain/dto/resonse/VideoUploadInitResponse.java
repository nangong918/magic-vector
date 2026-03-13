package com.openapi.domain.dto.resonse;

import lombok.Data;

@Data
public class VideoUploadInitResponse {
    private String uploadId;
    private Long uploadedOffset;
    private Integer chunkSize;
    private String message;
}
