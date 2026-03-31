package com.openapi.domain.dto.http;


import lombok.Data;

/**
 * RK 设备状态上报 DTO
 * SpringBoot透传，既是Request也是Response
 */
@Data
public class RkStatusDto {
    private String deviceId;
    private Integer battery;      // 电量百分比 0-100
    private String position;      // 位置坐标，如 "x:100,y:200"
    // 可扩展其他传感器数据
}
