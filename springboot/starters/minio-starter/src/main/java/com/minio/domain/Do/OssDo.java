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
    private String id = String.valueOf(IdUtil.getSnowflake().nextId());
    private String bucketName;
    private String objectName;
}
