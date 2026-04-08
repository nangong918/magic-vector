package com.openapi.component.manager.connect;

import com.openapi.interfaces.connect.Message;

import java.util.Map;

/**
 * @author 13225
 * @date 2025/11/13 17:25
 */
public class WebSocketMessage implements Message {

    private final String payload;
    public WebSocketMessage(String payload){
        this.payload = payload;
    }

    @Override
    public String getPayload() {
        return payload;
    }

    @Override
    public String getTopic() {
        return "websocket";
    }

    @Override
    public Map<String, Object> getHeaders() {
        return Map.of();
    }
}
