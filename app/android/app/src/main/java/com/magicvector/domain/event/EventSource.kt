package com.magicvector.domain.event

/**
 * 事件来源 - 精确区分数据变更的原因
 * 订阅者根据来源决定如何响应（如RoomManager根据来源决定是否更新数据库）
 */
open class EventSource {

    // ========== 用户操作 ==========
    sealed class UserAction : EventSource() {
        object AddOne : UserAction()      // 新增一条
        object DeleteOne : UserAction()   // 删除一条
        object DeleteAll : UserAction()   // 清空全部
        object UpdateOne : UserAction()   // 更新一条（不提供批量更新）
        object ChangeSort : UserAction()  // 切换排序方式
    }

    // ========== 远程请求 ==========
    sealed class Remote : EventSource() {
        object Full : Remote()  // Http全量
        object Page : Remote()  // Http分页
    }

    // ========== WebSocket ==========
    object WebSocket : EventSource()

    // ========== Room查询 ==========
    sealed class Room : EventSource() {
        object Full : Room()  // Room全量
        object Page : Room()  // Room分页
    }
}