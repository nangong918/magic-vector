package com.magicvector.domain.dto.ws.request

import com.magicvector.domain.dto.ws.base.CommonResultDto

data class StatusRequest(
    val deviceId: String
) : CommonResultDto()