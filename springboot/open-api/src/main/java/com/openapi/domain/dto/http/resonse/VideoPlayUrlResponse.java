package com.openapi.domain.dto.http.resonse;

import lombok.Data;

@Data
public class VideoPlayUrlResponse {
    private Long videoId;
    private String playUrl;
    private String status;
}
