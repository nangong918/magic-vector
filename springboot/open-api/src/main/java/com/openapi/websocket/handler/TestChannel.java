package com.openapi.websocket.handler;

import jakarta.servlet.http.HttpSession;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Component
@ServerEndpoint(value = "/test-channel")
public class TestChannel {

    //模拟储存当前用户的朋友全信息
    private  static final Map<String, Session> friendGroup = new ConcurrentHashMap<>();
    //存放当前用户信息
    private HttpSession httpSession;
    private Session session;

    // 收到消息
    @OnMessage
    public void onMessage(String message) throws IOException {

        log.info("[websocket] 收到消息：id={}，message={}", this.session.getId(), message);

        if (message.equalsIgnoreCase("bye")) {
            // 由服务器主动关闭连接。状态码为 NORMAL_CLOSURE（正常关闭）。
            this.session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Bye"));;
            return;
        }

        this.session.getAsyncRemote().sendText("["+ Instant.now().toEpochMilli() +"] Hello " + message);
    }

    // 连接打开
    @OnOpen
    public void onOpen(Session session, EndpointConfig endpointConfig){
        // 保存 session 到对象
        this.session = session;
        log.info("[websocket] 新的连接：id={}", this.session.getId());
    }

    // 连接关闭
    @OnClose
    public void onClose(CloseReason closeReason){
        log.info("[websocket] 连接断开：id={}，reason={}", this.session.getId(),closeReason);
    }

    // 连接异常
    @OnError
    public void onError(Throwable throwable) throws IOException {

        log.info("[websocket] 连接异常：id={}，throwable={}", this.session.getId(), throwable.getMessage());

        // 关闭连接。状态码为 UNEXPECTED_CONDITION（意料之外的异常）
        this.session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, throwable.getMessage()));
    }

}
