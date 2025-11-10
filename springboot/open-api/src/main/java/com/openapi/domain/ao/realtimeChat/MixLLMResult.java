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
                响应结构是JSON格式，需要根据句子进行拆分为JSONList，如果该句没有事件就只写chatSentence，
                如果该句话包括多个事件就需要将事件添加到eventList中。
                生成事件和调用tool要注意用户设定的权限。
                [
                    {"chatSentence":"xxx。"},
                    {
                        "chatSentence":"xxx。",
                        "eventList":[
                            {
                                "eventType":"motion",
                                "event": {
                                    "type": "前进",
                                    "value": "2"
                                }
                            },
                            {
                                "eventType":"motion",
                                "event": {
                                    "type": "左转",
                                    "value": "20.5"
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
