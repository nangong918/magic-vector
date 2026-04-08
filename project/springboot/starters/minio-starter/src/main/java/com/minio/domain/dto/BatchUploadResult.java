package com.minio.domain.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BatchUploadResult {
    private int successCount;
    private int failCount;
    private List<UploadItemResult> items = new ArrayList<>();
}
