package com.minio.domain.Do;

import cn.hutool.core.util.IdUtil;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

/**
 * @author 13225
 * @date 2025/9/29 16:03
 */
@Data
public class OssDo {
    @Id
    private Long id = IdUtil.getSnowflake().nextId();
    private Long userId;
    private String bucketName;
    private String objectName;
    private String originFileName;
    private String contentType;
    private Long fileSize;
    private String idempotentKey;
    private Long createdAt;
    private Long updatedAt;
}
