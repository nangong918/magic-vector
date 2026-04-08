package com.magicvector.domain.dto.ws.response

import com.magicvector.domain.dto.ws.base.StreamSeqDto

data class VlDataResponse(
    val content: String
) : StreamSeqDto()