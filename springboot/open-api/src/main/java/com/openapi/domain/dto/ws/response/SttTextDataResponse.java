package com.openapi.domain.dto.ws.response;

import com.openapi.domain.dto.ws.base.StreamSeqDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SttTextDataResponse extends StreamSeqDto {
    private String textStream;
}