package com.openapi.domain.dto.ws.request;

import com.openapi.domain.dto.ws.base.StreamSeqDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SttAudioDataRequest extends StreamSeqDto {
    private String base64AudioStream;
}