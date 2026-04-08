package com.openapi.domain.dto.http.resonse;

import lombok.Data;

@Data
public class VideoDownloadUrlResponse {
    private Long videoId;
    private String downloadUrl;
}
