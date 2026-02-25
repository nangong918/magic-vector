package com.data.domain.ao.mixLLM;


import com.data.domain.constant.tools.EquipmentEventType;

import java.util.Map;

/**
 * @author 13225
 * @date 2025/11/10 11:13
 */
public class MixLLMEvent {
    public String eventType = EquipmentEventType.NONE.code;
    public Map<String, String> event;
}
