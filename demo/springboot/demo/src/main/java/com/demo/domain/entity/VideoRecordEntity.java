package com.demo.domain.entity;

import cn.hutool.core.util.IdUtil;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class VideoRecordEntity {
    @Id
    private Long id = IdUtil.getSnowflake().nextId();
    private Long userId;
    private Long fileId;
    private String videoName;
    private Long fileSizeBytes;
    private Double durationSec;
    private Long bitrateKbps;
    private String objectName;
    private String coverObjectName;
    private String hlsObjectName;
    private String status;
    private String errorMessage;
    private Long createdAt;
    private Long updatedAt;
}
