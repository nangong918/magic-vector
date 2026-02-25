package com.magicvector.manager.ws

import android.util.Log
import com.data.domain.ao.chat.ChatWsTextMessageParseResult
import com.data.domain.constant.chat.RealtimeRequestDataTypeEnum
import com.data.domain.constant.chat.RealtimeResponseDataTypeEnum
import com.data.domain.constant.chat.RoleTypeEnum
import com.data.domain.dto.ws.reponse.RealtimeChatTextResponse
import com.data.domain.dto.ws.request.RealtimeChatConnectRequest
import com.google.gson.reflect.TypeToken
import com.magicvector.MainApplication
import com.magicvector.callback.OnReceiveAgentTextCallback
import com.magicvector.manager.ChatController
import com.magicvector.utils.chat.AbstractWsClient


object WsManager {

    const val TAG = "WebSocketManager"
    val GSON = MainApplication.GSON

    /**
     * 解析text文本信息
     * @param text  text文本信息
     * @return  ChatWsTextMessageParseResult    解析结果
     */
    fun getTextMessageDataType(text: String): ChatWsTextMessageParseResult?{
        if (text.isEmpty()) {
            Log.e(TAG, "handleTextMessage: text is empty")
            return null
        }

        var responseType: RealtimeResponseDataTypeEnum? = null
        var map: Map<String, String>? = null

        try {
            // text --GSON--> Map<String, String>
            map = GSON.fromJson(text, object : TypeToken<Map<String, String>>() {}.type)
            map?.let {
                val typeStr = map[RealtimeResponseDataTypeEnum.TYPE]
                typeStr?.let {
                    responseType = RealtimeResponseDataTypeEnum.getByType(it)
                }
            } ?: run {
                Log.e(TAG, "handleTextMessage: map is null")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleTextMessage: ", e)
            return null
        }

        if (responseType == null) {
            Log.e(TAG, "handleTextMessage: 解析responseType错误，responseType is null")
            return  null
        }

        return ChatWsTextMessageParseResult(responseType, map)
    }

    /**
     * 发送连接信息
     * @param agentId   agentId
     * @param userId    用户id
     * @param wsClient  发送消息的wsClient
     */
    fun sendOnOpenInfo(agentId: String, userId: String, wsClient: AbstractWsClient){
        val request = RealtimeChatConnectRequest()
        request.agentId = agentId
        request.userId = userId
        request.timestamp = System.currentTimeMillis()

        val dataMap = mapOf(
            RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.CONNECT.type,
            RealtimeRequestDataTypeEnum.DATA to GSON.toJson(request)
        )

        wsClient.sendMessage(
            messageMap = dataMap, isShowAllLog = true
        )
    }

    /**
     * 处理text文本信息
     * @param message                       text文本信息
     * @param chatControllerPointer         ChatController指针
     * @param onReceiveAgentTextCallback    接收代理文本的回调
     */
    fun handleTextMessage(message: String, chatControllerPointer: ChatController, onReceiveAgentTextCallback: OnReceiveAgentTextCallback?){
        var response : RealtimeChatTextResponse
        try {
            Log.i(TAG, "handleTextMessage::receiveMessage: $message")
            response = GSON.fromJson(message,
                RealtimeChatTextResponse::class.java)
        } catch (e: Exception){
            Log.e(TAG, "handleTextMessage::error: $message", e)
            return
        }

        if (response.role == null ||
            (response.role != RoleTypeEnum.USER.value && response.role != RoleTypeEnum.AGENT.value)){
            throw IllegalArgumentException("role is null or invalid")
        }

        response.content?.let {
            onReceiveAgentTextCallback?.onText(it)
            Log.i(TAG, "handleTextMessage::content: $it")
        }

        try {
            chatControllerPointer.setWsToViews(response)
        } catch (e: Exception){
            Log.e(TAG, "handleTextMessage::error: $message", e)
        }
    }

}