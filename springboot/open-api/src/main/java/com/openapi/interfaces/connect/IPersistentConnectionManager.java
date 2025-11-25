package com.openapi.interfaces.connect;

/**
 * @author 13225
 * @date 2025/11/13 17:55
 */
public interface IPersistentConnectionManager {
    void connect(ConnectionSession connectionSession);
    void disconnect();
    void onThrowable(Throwable throwable);
    void onMessage(Message message);
}
