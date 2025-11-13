package com.openapi.service;

import com.openapi.interfaces.connect.Message;

/**
 * @author 13225
 * @date 2025/11/13 17:44
 */
public interface PersistentConnectionService {
    void connect();
    void disconnect();
    void onThrowable(Throwable throwable);
    void onMessage(Message message);
}
