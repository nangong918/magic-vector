package com.openapi.interfaces.connect;

/**
 * @author 13225
 * @date 2025/11/13 16:17
 * 统一消息处理接口
 */
public interface MessageHandler {
    void handleMessage(Message message, ConnectionSession session);
}
