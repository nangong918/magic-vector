package com.magicvector.domain.event

/**
 * 事件来源 - 精确区分数据变更的原因
 * 订阅者根据来源决定如何响应（如RoomManager根据来源决定是否更新数据库）
 */
open class EventSource {

    /**
     * 1. 用户主动操作 (UI层)
     * 行为：置顶插入/更新
     */
    object UserAction : EventSource()

    /**
     * 2. Http全量请求 (RemoteApi)
     * 行为：全量替换
     */
    object RemoteFull : EventSource()

    /**
     * 3. Http分页请求 (RemoteApi)
     * 行为：有序插入历史数据
     */
    object RemotePage : EventSource()

    /**
     * 4. Ws长连接消息 (RealtimeChatController)
     * 行为：置顶插入最新消息
     */
    object WebSocket : EventSource()

    /**
     * 5. Room全量查询 (无网络+无内存缓存)
     * 行为：全量替换
     */
    object RoomFull : EventSource()

    /**
     * 6. Room分页查询 (无网络+有内存缓存)
     * 行为：有序插入历史数据
     */
    object RoomPage : EventSource()
}