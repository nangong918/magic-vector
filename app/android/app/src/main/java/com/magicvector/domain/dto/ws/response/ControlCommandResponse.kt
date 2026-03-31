package com.magicvector.domain.dto.ws.response

import com.magicvector.domain.dto.ws.base.CommonResultDto
import java.util.*

data class ControlCommandResponse(
    val commandId: String,
    val command: String,
    val params: Map<String, String>
) : CommonResultDto()