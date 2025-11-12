package com.minio.domain.ao;

import lombok.Data;

import java.io.Serializable;

/**
 * @author 13225
 * @date 2025/6/5 18:03
 */
@Data
public class FileAo implements Serializable {
    private String fileName;
    private String filePath;
    private Long fileSize;
}
