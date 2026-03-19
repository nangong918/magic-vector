package com.magicvector.domain.model.chat

import com.data.domain.constant.chat.RealtimeResponseDataTypeEnum

data class ChatWsTextMessageParseModel(
    val responseType: RealtimeResponseDataTypeEnum,
    val map: Map<String, String>
)
