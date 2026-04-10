package com.demo.domain.dto.http.resonse;

import lombok.Data;

@Data
public class OssFileContentUpdateResponse {
    private Long fileId;
    private String originFileName;
    private String url;
    private Boolean updated;
    private String message;
}
