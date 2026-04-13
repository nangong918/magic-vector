package com.minio.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class UploadItemResult {
    private String originFileName;
    private boolean success;
    private boolean duplicated;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long fileId;
    private String url;
    private String message;
}
