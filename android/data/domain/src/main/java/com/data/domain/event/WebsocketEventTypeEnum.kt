package com.data.domain.event


enum class WebsocketEventTypeEnum(val code: Int, val desc: String){
    // onOpen
    ON_OPEN(0, "onOpen"),
    // onMessage
    ON_MESSAGE(1, "onMessage"),
    // onMessage_byte
    ON_MESSAGE_BYTE(2, "onMessage_byte"),
    // onClosing
    ON_CLOSING(2, "onClosing"),
    // onClosed
    ON_CLOSED(3, "onClosed"),
    // onFailure
    ON_FAILURE(4, "onFailure"),
    // send Message
    SEND_MESSAGE(5, "sendMessage")
}