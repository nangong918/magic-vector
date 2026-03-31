package com.openapi.domain.dto.http.resonse;

import lombok.Data;

@Data
public class VideoUploadCompleteResponse {
    private Long videoId;
    private String objectName;
    private String message;
}
