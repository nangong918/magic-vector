package com.openapi.domain.dto.ws.base;

import lombok.Data;

@Data
public class StreamSeqDto {
    private String agentId;
    private String seq;          // 流序号
    private Boolean isLast;      // 是否最后一帧
    private Long timestamp;      // 时间戳
}