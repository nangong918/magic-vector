package com.magicvector.manager.ws

import android.util.Log
import com.magicvector.domain.model.chat.ChatWsTextMessageParseModel
import com.magicvector.domain.constant.chat.RealtimeRequestDataTypeEnum
import com.magicvector.domain.constant.chat.RealtimeResponseDataTypeEnum
import com.magicvector.domain.constant.chat.RoleTypeEnum
import com.magicvector.domain.dto.ws.response.WsChatTextResponse
import com.magicvector.domain.dto.ws.request.RealtimeChatBindChannelRequest
import com.magicvector.domain.dto.ws.request.RealtimeChatConnectRequest
import com.google.gson.reflect.TypeToken
import com.magicvector.MainApplication
import com.magicvector.domain.convertor.ChatMessageConvertor
import com.magicvector.domain.model.chat.ChatMessageModel
import com.magicvector.manager.event.chat.ChatEventMapManager
import com.magicvector.utils.chat.AbstractWsClient


object WsManager {

    const val TAG = "WebSocketManager"
    val GSON = MainApplication.GSON

    /**
     * 解析text文本信息
     * @param text  text文本信息
     * @return  ChatWsTextMessageParseResult    解析结果
     */
    fun getTextMessageDataType(text: String): ChatWsTextMessageParseModel?{
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

        return ChatWsTextMessageParseModel(responseType, map)
    }

    fun sendConnectInfo(userId: String, wsClient: AbstractWsClient){
        val request = RealtimeChatConnectRequest()
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

    fun sendBindChannelInfo(agentId: String, wsClient: AbstractWsClient){
        val request = RealtimeChatBindChannelRequest()
        request.agentId = agentId
        request.timestamp = System.currentTimeMillis()
        val dataMap = mapOf(
            RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.BIND_CHANNEL.type,
            RealtimeRequestDataTypeEnum.DATA to GSON.toJson(request)
        )
        wsClient.sendMessage(messageMap = dataMap, isShowAllLog = true)
    }

    /**
     * 处理text文本信息
     * @param message                       text文本信息
     * @param chatControllerPointer         ChatController指针
     * @param onReceiveAgentTextCallback    接收代理文本的回调
     */
    suspend fun handleTextMessage(message: String, chatEventMapManager: ChatEventMapManager){
        var response : WsChatTextResponse
        try {
            Log.i(TAG, "handleTextMessage::receiveMessage: $message")
            response = GSON.fromJson(message,
                WsChatTextResponse::class.java)
        } catch (e: Exception){
            Log.e(TAG, "handleTextMessage::error: $message", e)
            return
        }

        if (response.role != RoleTypeEnum.USER.value &&
            response.role != RoleTypeEnum.AGENT.value){
            throw IllegalArgumentException("role is null or invalid")
        }

        try {
            val agentId: Long = response.agentId.toLong()
            val chatEventManager = chatEventMapManager.getOrCreateManager(agentId)
            val chatMessageModel: ChatMessageModel = ChatMessageConvertor.wsChatTextResponse2Model(
                response = response
            )
            chatEventManager.onWsUpsertOne(item = chatMessageModel)
        } catch (e: Exception){
            Log.e(TAG, "handleTextMessage::error: $message", e)
        }
    }

}