package com.magicvector.domain.dto.ws.base

open class StreamSeqDto(
    open val agentId: String = "",
    open val seq: String = "",
    open val isLast: Boolean = false,
    open val timestamp: Long = 0L
)