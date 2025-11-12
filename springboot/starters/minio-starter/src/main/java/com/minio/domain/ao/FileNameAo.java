package com.minio.domain.ao;

import lombok.Data;

import java.io.Serializable;

/**
 * @author 13225
 * @date 2025/4/18 11:43
 */
@Data
public class FileNameAo implements Serializable {
    private Long userId;
    private String fileName;
    private Long timestamp;
}
