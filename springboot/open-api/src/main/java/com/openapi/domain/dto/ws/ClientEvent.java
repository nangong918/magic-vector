package com.openapi.domain.dto.ws;


import lombok.Data;
import java.util.Map;

/**
 * 客户端发送事件基类
 */
@Data
public abstract class ClientEvent {
    /**
     * 事件路由，见 WsChannel 枚举
     */
    private String channel;

    /**
     * 业务数据，key-value 格式
     */
    private Map<String, String> data;
}
