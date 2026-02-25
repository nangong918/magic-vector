package com.data.domain.ao.chat

import com.data.domain.constant.chat.RealtimeResponseDataTypeEnum

data class ChatWsTextMessageParseResult(
    val responseType: RealtimeResponseDataTypeEnum,
    val map: Map<String, String>
)
