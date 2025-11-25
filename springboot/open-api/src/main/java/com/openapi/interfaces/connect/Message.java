package com.openapi.interfaces.connect;

import java.util.Map;

/**
 * @author 13225
 * @date 2025/11/13 16:16
 * Websocket / Mqtt 统一消息接口
 */
public interface Message {
    String getPayload();
    // mqtt的
    String getTopic();
    // mqtt的
    Map<String, Object> getHeaders();
}
