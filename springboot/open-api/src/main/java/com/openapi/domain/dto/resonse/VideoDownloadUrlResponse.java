package com.openapi.domain.dto.resonse;

import lombok.Data;

@Data
public class VideoDownloadUrlResponse {
    private Long videoId;
    private String downloadUrl;
}
