package com.openapi.domain.dto.ws.base;


import com.openapi.domain.constant.ws.WsChannel;
import com.openapi.domain.constant.ws.WsEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Map;

/**
 * 客户端发送事件基类
 */
@Data
@AllArgsConstructor
public abstract class ClientEvent {
    /**
     * 事件路由，见 WsChannel 枚举
     * @see WsChannel
     */
    private String channel;
    /**
     * 事件名称，见 WsEvent 枚举
     * @see WsEvent
     */
    private String event;
    /**
     * 业务数据，key-value 格式
     */
    private Map<String, String> data;
}
