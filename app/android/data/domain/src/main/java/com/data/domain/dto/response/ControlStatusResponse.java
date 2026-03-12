package com.data.domain.dto.response;

public class ControlStatusResponse {
    public String deviceId;
    public Boolean appToSpringConnected;
    public Boolean rkToSpringConnected;
    public Boolean appToRkWifiConnected;
    public Boolean appToRkBleConnected;
    public String rkAgentMode;
    public Long lastHeartbeatAt;
}
