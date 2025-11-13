package com.openapi.component.manager.realTimeChat;

import com.openapi.interfaces.connect.ConnectionSession;
import com.openapi.interfaces.connect.IPersistentConnectionManager;
import com.openapi.interfaces.connect.Message;
import com.openapi.interfaces.connect.PersistentConnection;
import lombok.extern.slf4j.Slf4j;


/**
 * @author 13225
 * @date 2025/11/13 17:52
 */
@Slf4j
public class PersistentConnectionManager implements IPersistentConnectionManager {

    private PersistentConnection connection;

    @Override
    public void connect(PersistentConnection connection) {
        // WebSocket 连接逻辑（通常由前端或客户端发起）
        log.info("[WebSocketConnection] connect");
        this.connection = connection;
    }

    @Override
    public void disconnect() {
        if (connection.getSession().isConnected()){
            connection.getSession().close();
        }
    }

    @Override
    public void onThrowable(Throwable throwable) {
        log.error("[WebSocketConnection] onThrowable error", throwable);
    }

    @Override
    public void onMessage(Message message) {

    }

}
