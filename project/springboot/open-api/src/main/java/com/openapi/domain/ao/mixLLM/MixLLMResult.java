package com.openapi.domain.ao.mixLLM;

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
    // 客户端阶段排序：同 timing 并行，跨 timing 串行
    public Integer instructionTiming;

    @JsonIgnore
    public static String getInvocationRules() {
        return """
                响应结构是 JSONList, 且必须按「MCP 事件」做模块切分：
                1) 含 MCP 的句子: 该句单独一个模块(chatSentence + eventList)。
                2) 不含 MCP 的句子：连续无 MCP 的句子合并为一个模块，直到下一个 MCP 前结束。
                3) 每个模块都要有 instructionTiming, 必须从 0 递增。
                [
                    {"chatSentence":"xxx。","instructionTiming":0},
                    {
                        "chatSentence":"xxx。",
                        "instructionTiming":1,
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
