package com.openapi.domain.dto.ws.response;

import com.openapi.domain.dto.ws.base.CommonResultDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class SystemMessageResponse extends CommonResultDto {
    private String event;
    private Map<String, String> param;
}