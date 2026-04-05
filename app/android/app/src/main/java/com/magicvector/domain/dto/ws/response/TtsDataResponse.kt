package com.magicvector.domain.dto.ws.response

import com.magicvector.domain.dto.ws.base.StreamSeqDto

data class TtsDataResponse(
    val base64AudioStream: String,
    /** 与当前音频分片对应的字幕/文案（流式 TTS 可与播放同步展示） */
    val text: String = "",
) : StreamSeqDto()