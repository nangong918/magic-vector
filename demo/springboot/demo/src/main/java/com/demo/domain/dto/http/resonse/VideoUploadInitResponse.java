package com.demo.domain.dto.http.resonse;

import lombok.Data;

@Data
public class VideoUploadInitResponse {
    private String sessionId;
    private Long uploadedBytes;
    private Long totalBytes;
    private String message;
}
