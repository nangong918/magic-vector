package com.openapi.domain.dto.ws.response;

import com.openapi.domain.dto.ws.base.StreamSeqDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TtsDataResponse extends StreamSeqDto {

    /**
     * 与当前音频分片对应的字幕/文案（流式 TTS 可与播放同步展示）。
     */
    private String text;

    private String base64AudioStream;
}