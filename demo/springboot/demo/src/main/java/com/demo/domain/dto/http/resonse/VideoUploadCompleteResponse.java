package com.demo.domain.dto.http.resonse;

import lombok.Data;

@Data
public class VideoUploadCompleteResponse {
    private String sessionId;
    private String fileId;
    private String url;
    private String message;
}
