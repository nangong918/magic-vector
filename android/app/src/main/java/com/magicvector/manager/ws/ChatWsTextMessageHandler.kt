package com.magicvector.manager.ws

import android.util.Log
import com.data.domain.constant.chat.RoleTypeEnum
import com.data.domain.dto.ws.reponse.RealtimeChatTextResponse
import com.google.gson.Gson
import com.magicvector.callback.OnReceiveAgentTextCallback
import com.magicvector.manager.ChatController

object ChatWsTextMessageHandler {

    const val TAG = "ChatWsTextMessageHandler"

    fun handleTextMessage(message: String, gson: Gson, chatControllerPointer: ChatController, onReceiveAgentTextCallback: OnReceiveAgentTextCallback?){
        var response : RealtimeChatTextResponse
        try {
            Log.i(TAG, "handleTextMessage::receiveMessage: $message")
            response = gson.fromJson(message,
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