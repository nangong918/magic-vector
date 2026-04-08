package com.magicvector.domain.dto.ws.response

import com.magicvector.domain.dto.ws.base.CommonResultDto

data class VlErrorResponse(
    val agentId: String,
    override val requestId: String = "",
    override val code: Int = 0,
    override val message: String = ""
) : CommonResultDto()