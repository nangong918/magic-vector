package com.openapi.domain.evnet.body;

import com.openapi.domain.dto.ws.response.SystemTextResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author 13225
 * @date 2025/10/29 14:07
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TakePhotoEventBody {
    private SystemTextResponse systemTextResponse;
}