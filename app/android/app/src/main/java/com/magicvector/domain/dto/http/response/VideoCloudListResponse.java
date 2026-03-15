package com.magicvector.domain.dto.http.response;

import java.util.ArrayList;
import java.util.List;

public class VideoCloudListResponse {
    public List<VideoCloudItem> videos = new ArrayList<>();

    public static class VideoCloudItem {
        public Long videoId;
        public String objectName;
        public String hlsObjectName;
        public String status;
        public Long createdAt;
    }
}
