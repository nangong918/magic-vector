package com.demo.domain.dto.http.resonse;

import com.minio.domain.bo.UploadItemResult;
import lombok.Data;

import java.util.List;

@Data
public class OssBatchUploadResponse {
    private Long userId;
    private String bucketName;
    private Integer successCount;
    private Integer failCount;
    private List<UploadItemResult> items;
}
