package com.magicvector.domain.dto.ws.response

import com.magicvector.domain.dto.ws.base.StreamSeqDto

data class TtsDataResponse(
    val base64AudioStream: String
) : StreamSeqDto()