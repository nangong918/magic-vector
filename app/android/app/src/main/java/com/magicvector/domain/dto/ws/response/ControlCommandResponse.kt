package com.magicvector.domain.dto.ws.response

import com.magicvector.domain.dto.ws.base.CommonResultDto
import java.util.*

data class ControlCommandResponse(
    val commandId: String,
    val command: String,
    val params: Map<String, String>,
    /**
     * 服务端下行 `control_command_sb` 时的推送目标，与 [com.magicvector.domain.constant.ws.WsPushTarget.value] 一致。
     */
    val target: String = "",
) : CommonResultDto()