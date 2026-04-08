package com.openapi.domain.dto.ws.base;

import com.openapi.domain.constant.ws.ClientType;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 客户端会话信息
 */
@Data
public class ClientSession {
    private String sessionId;
    private WebSocketSession webSocketSession;
    private ClientType clientType;
    private String clientId;      // APP: userId, RK: deviceId
    private Long connectTime;
    private Long lastHeartbeat;

    // 发送队列（待发送给客户端的消息）
    private BlockingQueue<ServerEvent> sendQueue = new LinkedBlockingQueue<>(32);

    // 接收队列（从客户端收到的消息，用于异步处理）
    private BlockingQueue<ClientEvent> receiveQueue = new LinkedBlockingQueue<>(32);
}