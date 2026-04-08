package com.openapi.domain.Do;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class VideoRecordDo {
    @Id
    private Long id;
    private Long userId;
    private String objectName;
    private String hlsObjectName;
    private String status;
    private Long createdAt;
    private Long updatedAt;
}
