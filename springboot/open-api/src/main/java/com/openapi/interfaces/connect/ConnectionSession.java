package com.openapi.interfaces.connect;

/**
 * @author 13225
 * @date 2025/11/13 16:12
 * Websocket / Mqtt 统一会话接口
 */
public interface ConnectionSession {
    void send(String payload);
    String getSessionId();
    boolean isConnected();
    void close();
}
