package com.minio.domain.ao;

import lombok.Data;

import java.io.Serializable;

/**
 * @author 13225
 * @date 2025/4/18 11:51
 */
@Data
public class ErrorFile implements Serializable {
    private String objectName;
    private Long fileId = null;
    private String errorMessage;

    public ErrorFile() {
    }

    public ErrorFile(String objectName, String errorMessage) {
        this.objectName = objectName;
        this.errorMessage = errorMessage;
    }

    public ErrorFile(String objectName, Long fileId, String errorMessage) {
        this.objectName = objectName;
        this.fileId = fileId;
        this.errorMessage = errorMessage;
    }
}
