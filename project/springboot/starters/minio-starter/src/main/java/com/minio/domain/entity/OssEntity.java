package com.minio.domain.entity;

import cn.hutool.core.util.IdUtil;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class OssEntity {
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
