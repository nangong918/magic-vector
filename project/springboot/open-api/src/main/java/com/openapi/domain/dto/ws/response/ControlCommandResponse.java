package com.openapi.domain.dto.ws.response;

import com.openapi.domain.dto.ws.base.CommonResultDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class ControlCommandResponse extends CommonResultDto {

    /**
     * 服务端下行 {@code control_command_sb} 时的推送目标，与 {@link com.openapi.domain.constant.ws.WsPushTarget#getValue()} 一致，
     * 例如发往 App 或 RK，由客户端校验后处理。
     */
    public String target;

    private String commandId;
    private String command;
    private Map<String, String> params;
}