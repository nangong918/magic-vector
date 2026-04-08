package com.magicvector.domain.dto.ws.request

import com.magicvector.domain.dto.ws.base.CommonResultDto
import java.util.*

data class ControlCommandRequest(
    val deviceId: String,
    val command: String,
    val params: Map<String, String>
) : CommonResultDto()