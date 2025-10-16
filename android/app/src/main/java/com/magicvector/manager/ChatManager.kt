package com.magicvector.manager

import android.util.Log
import com.core.baseutil.date.DateUtils
import com.data.domain.Do.ChatMessageDo
import com.data.domain.ao.chat.ChatItemAo
import com.data.domain.constant.chat.MessageTypeEnum
import com.data.domain.dto.ws.RealtimeChatTextResponse

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

    companion object {
        val TAG = ChatManager::class.simpleName
    }

    // response
    val responseChatMessageList: MutableList<ChatMessageDo> = mutableListOf()
    // ws
    val wsChatMessageList: MutableList<RealtimeChatTextResponse> = mutableListOf()
    // view
    val viewChatMessageList: MutableList<ChatItemAo> = mutableListOf()

    // response -> view
    private fun responsesToViews(responses: List<ChatMessageDo>){
        for (response in responses) {
            // 只有 ChatItemAo 中不包含此条消息才添加
            var isExist = false
            for (viewChatMessage in viewChatMessageList){
                if (viewChatMessage.messageId == response.id){
                    isExist = true
                    break
                }
            }
            if (!isExist) {
                val view = responseToView(response)
                viewChatMessageList.add(view)
            }
        }
    }

    private fun responseToView(response: ChatMessageDo): ChatItemAo{
        val ao = ChatItemAo()
        // view
        // todo 暂时不支持发送图片
//        ao.vo.imgUrl = response.imgUrl
        ao.vo.content = response.content
        ao.vo.time = runCatching {
            DateUtils.yyyyMMddHHmmssToString(response.chatTime)
        }.getOrElse {
            // 记录异常信息（可选）
            Log.e(TAG, "时间转换失败")
            ""
        }
        ao.vo.viewType = response.role
        ao.vo.messageType = MessageTypeEnum.TEXT.value

        // data
        ao.senderId = response.agentId
        ao.receiverId = response.userId
        ao.messageId = response.id
        ao.timestamp = response.chatTimestamp

        return ao
    }

    // ws -> view
    private fun wssToViews(wss: List<RealtimeChatTextResponse>){
        for (ws in wss) {
            // 只有 ChatItemAo 中不包含此条消息才添加
            var isExist = false
            for (view in viewChatMessageList) {
                if (view.messageId == ws.messageId) {
                    isExist = true
                    break
                }
            }
            if (!isExist) {
                val view = wsToView(ws)
                viewChatMessageList.add(view)
            }
        }
    }

    private fun wsToView(ws: RealtimeChatTextResponse): ChatItemAo{
        val ao = ChatItemAo()
        // view
        // todo 暂时不支持发送图片
//        ao.vo.imgUrl = ""
        ao.vo.content = ws.content
        ao.vo.time = runCatching {
            DateUtils.yyyyMMddHHmmssToString(ws.chatTime)
        }.getOrElse {
            // 记录异常信息（可选）
            Log.e(TAG, "时间转换失败")
            ""
        }
        ao.vo.viewType = ws.role
        ao.vo.messageType = MessageTypeEnum.TEXT.value


        // data
        ao.senderId = ws.agentId
        ao.receiverId = ws.userId
        ao.messageId = ws.messageId
        ao.timestamp = ws.timestamp

        return ao
    }

}