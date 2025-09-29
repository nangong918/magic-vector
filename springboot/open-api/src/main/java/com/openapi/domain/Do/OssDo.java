package com.openapi.domain.Do;

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
    private String id;
    private String bucketName;
    private String objectName;

    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
}
