package com.demo.domain.dto.http.resonse;

import lombok.Data;

@Data
public class OssFileNameUpdateResponse {
    private Long fileId;
    private String newFileName;
    private Boolean updated;
    private String message;
}
