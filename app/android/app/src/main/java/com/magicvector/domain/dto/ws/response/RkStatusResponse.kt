package com.magicvector.domain.dto.ws.response

import com.magicvector.domain.dto.ws.base.CommonResultDto

data class RkStatusResponse(
    val deviceId: String,
    val battery: Int,
    val position: String
) : CommonResultDto()