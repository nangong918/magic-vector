package com.magicvector.domain.dto.ws.request

import com.magicvector.domain.dto.ws.base.StreamSeqDto

data class SttAudioDataRequest(
    val base64AudioStream: String
) : StreamSeqDto()