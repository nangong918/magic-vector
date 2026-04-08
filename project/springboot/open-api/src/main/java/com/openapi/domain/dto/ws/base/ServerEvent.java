package com.openapi.domain.dto.ws.base;

import com.openapi.domain.constant.ws.WsEvent;
import com.openapi.domain.constant.ws.WsChannel;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * 服务端发送的事件
 */
@Data
@AllArgsConstructor
public class ServerEvent {
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
