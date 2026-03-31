package com.magicvector.domain.dto.ws.response

import com.magicvector.domain.dto.ws.base.CommonResultDto

data class ControlResponse(
    val deviceId: String
) : CommonResultDto()