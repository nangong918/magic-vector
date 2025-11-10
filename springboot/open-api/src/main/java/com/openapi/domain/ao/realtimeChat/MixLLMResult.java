package com.openapi.domain.ao.realtimeChat;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @JsonIgnore
    public static String getInvocationRules() {
        return """
                响应结构是JSON格式，需要根据句子进行拆分为JSONList，如果该句没有事件就只赋值chatSentence，如果该句话包括多个事件就需要将事件添加到eventList中。
                [
                    {"chatSentence":"xxx。"},
                    {
                        "chatSentence":"xxx。",
                        "eventList":[
                            {
                                "eventType":"motion",
                                "event": {
                                    "type": "停止"
                                }
                            },
                            {
                                "eventType":"emoji",
                                "event": {
                                    "type": "眨眼"
                                }
                            }
                        ]
                    }
                ]
                """;
    }
}
