package com.openapi.domain.dto.ws.base;

import lombok.Data;

/**
 * 队列消息包装
 */
@Data
public class QueueMessage<T> {
    private String id;
    private Long timestamp;
    private T payload;

    public QueueMessage(T payload) {
        this.id = java.util.UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.payload = payload;
    }
}
