package com.magicvector.manager

import android.util.Log
import com.data.domain.constant.chat.RoleTypeEnum
import com.data.domain.dto.ws.RealtimeChatTextResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.magicvector.callback.VADCallTextCallback

object ChatWsTextMessageHandler {

    const val TAG = "ChatWsTextMessageHandler"

    fun handleTextMessage(message: String, GSON: Gson, chatManagerPointer: ChatManager, vadCallTextCallback: VADCallTextCallback){
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
            vadCallTextCallback.onText(it)
            Log.i(TAG, "handleTextMessage::content: $it")
        }

        try {
            chatManagerPointer.setWsToViews(response)
        } catch (e: Exception){
            Log.e(TAG, "handleTextMessage::error: $message", e)
        }
    }

}