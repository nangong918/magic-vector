package com.data.domain.event

import java.io.Serializable

class WebSocketMessageEvent(
    val text: String,
    val eventType: WebsocketEventTypeEnum
) : Serializable {
}