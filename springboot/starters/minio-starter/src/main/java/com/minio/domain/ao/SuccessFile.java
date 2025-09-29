package com.minio.domain.ao;

import lombok.Data;

import java.io.Serializable;

/**
 * @author 13225
 * @date 2025/4/22 16:37
 */
@Data
public class SuccessFile implements Serializable {
    private String fileName;
    private String objectName;
    private Long fileSize;
    private String fileId = null;
    public SuccessFile(){

    }
    public SuccessFile(String fileName, String objectName, Long fileSize) {
        this.fileName = fileName;
        this.objectName = objectName;
        this.fileSize = fileSize;
    }

    public SuccessFile(String fileName, String objectName, Long fileSize, String fileId) {
        this.fileName = fileName;
        this.objectName = objectName;
        this.fileSize = fileSize;
        this.fileId = fileId;
    }
}
