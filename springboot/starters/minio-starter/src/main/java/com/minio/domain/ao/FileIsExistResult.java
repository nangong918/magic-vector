package com.minio.domain.ao;

import lombok.Data;

import java.io.Serializable;

/**
 * @author 13225
 * @date 2025/4/22 16:56
 */
@Data
public class FileIsExistResult implements Serializable {
    // 是否存在？
    public Boolean isExist = false;
    // 如果存再找到id
    public String fileId = null;
}
