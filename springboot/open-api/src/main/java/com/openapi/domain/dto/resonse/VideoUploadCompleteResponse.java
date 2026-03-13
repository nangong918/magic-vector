package com.openapi.domain.dto.resonse;

import lombok.Data;

@Data
public class VideoUploadCompleteResponse {
    private Long videoId;
    private String objectName;
    private String message;
}
