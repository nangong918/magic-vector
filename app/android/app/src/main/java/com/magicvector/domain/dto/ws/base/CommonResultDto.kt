package com.magicvector.domain.dto.ws.base

open class CommonResultDto(
    open val requestId: String = "",
    open val code: Int = 0,
    open val message: String = ""
)