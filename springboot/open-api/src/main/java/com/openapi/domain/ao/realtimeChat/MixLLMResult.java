package com.openapi.domain.ao.realtimeChat;

import lombok.Data;

import java.util.List;

/**
 * @author 13225
 * @date 2025/11/10 11:10
 */
@Data
public class MixLLMResult {
    // chat结果
    public String chatSentence;
    // eventList
    public List<MixLLMEvent> eventList;
}
