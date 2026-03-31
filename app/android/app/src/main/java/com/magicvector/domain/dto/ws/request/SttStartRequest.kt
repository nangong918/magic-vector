package com.magicvector.domain.dto.ws.request

import com.magicvector.domain.dto.ws.base.CommonResultDto

data class SttStartRequest(
    override val requestId: String = "",
    override val code: Int = 0,
    override val message: String = ""
) : CommonResultDto()