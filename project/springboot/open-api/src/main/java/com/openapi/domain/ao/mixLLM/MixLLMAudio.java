package com.openapi.domain.ao.mixLLM;

import lombok.Data;

import java.util.List;

/**
 * @author 13225
 * @date 2025/11/10 18:20
 */
@Data
public class MixLLMAudio {
    // audioResult
    public String base64Audio;
    // eventList (只有在第一个Audio开始的时候有)
    public List<MixLLMEvent> eventList;
}
