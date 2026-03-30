package com.openapi.domain.dto.ws;

import lombok.Data;

import java.util.Map;

/**
 * 服务端发送的事件
 */
@Data
public class ServerEvent {
    /**
     * 事件路由，见 WsChannel 枚举
     */
    private String channel;

    /**
     * 业务数据，key-value 格式
     */
    private Map<String, String> data;
}
