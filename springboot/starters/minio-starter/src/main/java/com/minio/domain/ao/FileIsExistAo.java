package com.minio.domain.ao;

import lombok.Data;

import java.io.Serializable;

/**
 * @author 13225
 * @date 2025/6/11 16:23
 */
@Data
public class FileIsExistAo implements Serializable {
    private Long userId;
    private String objectName;
    private String bucketName;
    private Long fileSize;
}
