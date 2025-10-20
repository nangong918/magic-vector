package com.magicvector.manager

import android.util.Log
import com.data.domain.constant.chat.RoleTypeEnum
import com.data.domain.dto.ws.RealtimeChatTextResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ChatWsTextMessageHandler {

    const val TAG = "ChatWsTextMessageHandler"

    fun handleTextMessage(message: String, GSON: Gson, chatManagerPointer: ChatManager){
        var response : RealtimeChatTextResponse
        try {
            Log.i(TAG, "handleTextMessage: $message")
            response = GSON.fromJson(message,
                RealtimeChatTextResponse::class.java)
        } catch (e: Exception){
            Log.e(TAG, "handleTextMessage: $message", e)
            return
        }

        if (response.role == null ||
            (response.role != RoleTypeEnum.USER.value && response.role != RoleTypeEnum.AGENT.value)){
            throw IllegalArgumentException("role is null or invalid")
        }

        response.content?.let {
            Log.i(TAG, "handleTextMessage: $it")
        }
        chatManagerPointer.setWssToViews(listOf(response))
    }

}