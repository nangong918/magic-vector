package com.openapi.domain.dto.resonse;

import lombok.Data;

@Data
public class ControlStatusResponse {
    private String deviceId;
    private Boolean appToSpringConnected;
    private Boolean rkToSpringConnected;
    private Boolean appToRkWifiConnected;
    private Boolean appToRkBleConnected;
    private String rkAgentMode;
    private Long lastHeartbeatAt;
}
