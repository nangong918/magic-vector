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
            val type = object : TypeToken<RealtimeChatTextResponse>() {}.type
            response = GSON.fromJson(message, type)
        } catch (e: Exception){
            Log.e(TAG, "handleTextMessage: $message", e)
            return
        }

        if (response.role == null ||
            (response.role != RoleTypeEnum.USER.value && response.role != RoleTypeEnum.AGENT.value)){
            throw IllegalArgumentException("role is null or invalid")
        }

        chatManagerPointer.wssToViews(listOf(response))
    }

}