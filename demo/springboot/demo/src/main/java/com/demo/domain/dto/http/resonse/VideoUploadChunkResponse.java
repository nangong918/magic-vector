package com.demo.domain.dto.http.resonse;

import lombok.Data;

@Data
public class VideoUploadChunkResponse {
    private String sessionId;
    private Long uploadedBytes;
    private Long totalBytes;
    private Boolean completed;
}
