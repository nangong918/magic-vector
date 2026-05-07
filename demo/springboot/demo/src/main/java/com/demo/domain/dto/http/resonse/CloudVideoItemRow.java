package com.demo.domain.dto.http.resonse;

import lombok.Data;

@Data
public class CloudVideoItemRow {
    private String fileId;
    private String fileName;
    private String fileSize;
    private String durationSec;
    private String bitrateKbps;
    private String coverUrl;
    private String hlsUrl;
    private String mp4DownloadUrl;
}
