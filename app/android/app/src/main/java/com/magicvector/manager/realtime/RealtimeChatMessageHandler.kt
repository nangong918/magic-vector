package com.magicvector.manager.realtime


import android.util.Log
import com.data.domain.ao.mixLLM.MixLLMEvent
import com.magicvector.domain.constant.chat.MessageTypeEnum
import com.magicvector.domain.constant.chat.RealtimeResponseDataTypeEnum
import com.magicvector.domain.constant.chat.RealtimeSystemResponseEventEnum
import com.google.gson.reflect.TypeToken
import com.magicvector.MainApplication
import com.magicvector.domain.constant.VadChatState
import com.magicvector.domain.model.chat.ChatMessageModel
import com.magicvector.domain.vo.message.ChatBriefMessageVO
import com.magicvector.domain.vo.message.ChatMessageVO
import com.magicvector.manager.event.chat.ChatEventManager
import com.magicvector.manager.mcp.HandleSystemResponse
import com.magicvector.manager.ws.WsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * WebSocket 消息处理器
 * 负责解析和处理各类实时消息
 */
class RealtimeChatMessageHandler(
    private val coroutineScope: CoroutineScope,
    private val eventFlow: RealtimeChatEventFlow,
    private val audioManager: RealtimeChatAudioManager,
    private val chatEventManager: ChatEventManager?,
    private var agentId: Long?,
    private var userId: Long?
) {
    companion object {
        const val TAG = "RealtimeChatMessageHandler"
        val GSON = MainApplication.GSON
    }

    var handleSystemResponse: HandleSystemResponse? = null

    /**
     * 处理收到的文本消息
     */
    fun handleTextMessage(text: String) {
        val chatWsTextMessageParseResult = WsManager.getTextMessageDataType(text) ?: return
        val type = chatWsTextMessageParseResult.responseType
        val map = chatWsTextMessageParseResult.map

        when (type) {
            RealtimeResponseDataTypeEnum.START_TTS -> handleStartTts()
            RealtimeResponseDataTypeEnum.STOP_TTS -> handleStopTts()
            RealtimeResponseDataTypeEnum.AUDIO_CHUNK -> handleAudioChunk(map)
            RealtimeResponseDataTypeEnum.TEXT_CHAT_RESPONSE -> handleTextChatResponse(map)
            RealtimeResponseDataTypeEnum.TEXT_SYSTEM_RESPONSE -> handleSystemMessage(map)
            RealtimeResponseDataTypeEnum.EVENT_LIST -> handleEventList(map)
            else -> {}
        }
    }

    // ========== 私有处理方法 ==========

    private fun handleStartTts() {
        eventFlow.updateRealtimeState(RealtimeChatState.Receiving)
        eventFlow.emitVadState(VadChatState.Replying)
        audioManager.startAudioTrackPlay()
    }

    private fun handleStopTts() {
        eventFlow.updateRealtimeState(RealtimeChatState.InitializedConnected)
        eventFlow.emitVadState(VadChatState.Silent)
        audioManager.stopAudioTrackPlay()
    }

    private fun handleAudioChunk(map: Map<String, String>) {
        eventFlow.updateRealtimeState(RealtimeChatState.Receiving)
        val data = map[RealtimeResponseDataTypeEnum.DATA]
        data?.let {
            audioManager.playBase64Audio(base64Audio = it, isShowLog = true)
        }
    }

    private fun handleTextChatResponse(map: Map<String, String>) {
        eventFlow.updateRealtimeState(RealtimeChatState.Receiving)
        val data = map[RealtimeResponseDataTypeEnum.DATA]
        data?.let {
            handleTextMessageResponse(it)
        }
    }

    private fun handleTextMessageResponse(message: String) {
        val response = runCatching {
            GSON.fromJson(message, com.magicvector.domain.dto.ws.response.WsChatTextResponse::class.java)
        }.getOrNull() ?: return

        // 构建 ChatMessageModel
        val chatMessage = ChatMessageModel(
            chatMessageVo = ChatMessageVO(
                briefMessageVo = ChatBriefMessageVO(
                    content = response.content,
                    chatTime = response.chatTime,
                    role = response.role
                ),
                imgUrl = "",
                messageType = MessageTypeEnum.TEXT.value
            ),
            agentId = response.agentId.toLongOrNull() ?: agentId ?: 0L,
            userId = response.userId.toLongOrNull() ?: userId ?: 0L,
            messageId = response.messageId.toLongOrNull() ?: 0L,
            timestamp = response.timestamp
        )

        coroutineScope.launch {
            chatEventManager?.onWsUpsertOne(chatMessage)
            eventFlow.emitAgentText(response.content)
        }
    }

    private fun handleSystemMessage(map: Map<String, String>) {
        val data = map[RealtimeResponseDataTypeEnum.DATA] ?: return
        try {
            val systemMap: Map<String, String> = GSON.fromJson(data, object : TypeToken<Map<String, String>>() {}.type)
            if (systemMap[RealtimeSystemResponseEventEnum.EVENT_KET] != null) {
                handleSystemResponse?.handleSystemResponse(systemMap)
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleSystemMessage: parse error", e)
        }
    }

    private fun handleEventList(map: Map<String, String>) {
        val data = map[RealtimeResponseDataTypeEnum.DATA] ?: return
        try {
            val eventList: List<MixLLMEvent> = GSON.fromJson(data, object : TypeToken<List<MixLLMEvent>>() {}.type)
            eventList.forEach { event ->
                Log.d(TAG, "Event Type: ${event.eventType}, Event Data: ${event.event}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleEventList: parse error", e)
        }
    }
}