package com.magicvector.domain.dto.ws.response

import com.magicvector.domain.dto.ws.base.StreamSeqDto

data class SttTextDataResponse(
    val textStream: String
) : StreamSeqDto()