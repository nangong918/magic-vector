package com.demo.domain.dto.http.resonse;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.minio.domain.bo.UploadItemResult;
import lombok.Data;

import java.util.List;

@Data
public class OssBatchUploadResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;
    private String bucketName;
    private Integer successCount;
    private Integer failCount;
    private List<UploadItemResult> items;
}
