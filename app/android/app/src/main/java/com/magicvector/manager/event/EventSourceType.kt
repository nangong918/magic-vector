package com.magicvector.manager.event

enum class EventSourceType {
    USER_ACTION,   // 用户行为
    HTTP_FULL,      // HTTP 全量
    HTTP_PAGE,      // HTTP 分页
    WS_REALTIME,    // WebSocket 实时
    ROOM_FULL,      // room 全量
    ROOM_PAGE,      // room 分页
    MEMORY_CACHE,   // 内存缓存
    COMPATIBILITY
}
