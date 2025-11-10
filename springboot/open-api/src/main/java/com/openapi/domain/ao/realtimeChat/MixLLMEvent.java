package com.openapi.domain.ao.realtimeChat;

import com.openapi.domain.constant.tools.EquipmentEventType;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author 13225
 * @date 2025/11/10 11:13
 */
@Data
public class MixLLMEvent {
    public String eventType = EquipmentEventType.NONE.code;
    // todo 后续设计event事件list
    public List<Map<String, String>> eventList;
}
