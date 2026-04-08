package com.openapi.domain.dto.http.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatByAnchorRequest {
    @NotBlank(message = "agentId不能为空")
    private String agentId;

    @NotNull(message = "anchorTimestamp不能为空")
    @Min(value = 0, message = "anchorTimestamp必须>=0")
    private Long anchorTimestamp;

    /**
     * true: 向前拉历史(chat_timestamp < anchor)
     * false: 向后拉补偿(chat_timestamp > anchor)
     */
    @NotNull(message = "before不能为空")
    private Boolean before;

    @NotNull(message = "limit不能为空")
    @Min(value = 1, message = "limit必须>=1")
    private Integer limit;
}
