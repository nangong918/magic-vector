package com.demo.domain.dto.http.resonse;

import lombok.Data;

@Data
public class VideoHlsToMp4Response {
    private String fileId;
    private String downloadUrl;
    private String message;
}
