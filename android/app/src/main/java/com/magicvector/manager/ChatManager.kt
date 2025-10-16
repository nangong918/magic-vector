package com.magicvector.manager

import com.data.domain.Do.ChatMessageDo
import com.data.domain.ao.chat.ChatItemAo

/**
 * ChatManager
 * chat信息来源：后端，本地
 * 后端：
 *  1.ChatRequest请求获取List
 *  2.Websocket收到的Chat消息（还是他妈流式的）
 * 本地：
 *  1.Room
 *  2.MMKV
 */
class ChatManager {
    // request
    val requestChatMessageList: MutableList<ChatMessageDo> = mutableListOf()
    // ws todo 前后端确定好数据格式
    // view
    val viewChatMessageList: MutableList<ChatItemAo> = mutableListOf()
}