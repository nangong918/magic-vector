package com.magicvector.domain.dto.http.request;

import java.io.Serializable;

public class ControlCommandRequest implements Serializable {
    public String userId;
    public String deviceId;
    public String transport;
    public String commandType;
    public Long sequence;
    public Long timestamp;
    public String payloadJson;
}
