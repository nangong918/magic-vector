package com.magicvector.domain.dto.ws.request

import com.magicvector.domain.dto.ws.base.CommonResultDto

data class RkConnectRequest(
    val deviceId: String
) : CommonResultDto()