package com.openapi.domain.dto.resonse;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class VideoCloudListResponse {
    private List<VideoCloudItem> videos = new ArrayList<>();

    @Data
    public static class VideoCloudItem {
        private Long videoId;
        private String objectName;
        private String hlsObjectName;
        private String status;
        private Long createdAt;
    }
}
