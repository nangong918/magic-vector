package com.magicvector.domain.model.chat

import com.magicvector.domain.constant.chat.RealtimeResponseDataTypeEnum

data class ChatWsTextMessageParseModel(
    val responseType: RealtimeResponseDataTypeEnum,
    val map: Map<String, String>
)
