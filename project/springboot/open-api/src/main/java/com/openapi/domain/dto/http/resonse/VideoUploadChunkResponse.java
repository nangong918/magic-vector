package com.openapi.domain.dto.http.resonse;

import lombok.Data;

@Data
public class VideoUploadChunkResponse {
    private String uploadId;
    private Long uploadedOffset;
    private Boolean accepted;
    private String message;
}
