package com.openapi.domain.dto.ws.response;

import com.openapi.domain.dto.ws.base.StreamSeqDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TtsDataResponse extends StreamSeqDto {
    private String base64AudioStream;
}