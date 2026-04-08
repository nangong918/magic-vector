package com.magicvector.domain.dto.ws.response

import com.magicvector.domain.dto.ws.base.CommonResultDto

data class SystemMessageResponse(
    val event: String,
    val param: Map<String, String>
) : CommonResultDto()