package com.openapi.component.manager.mixLLM;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.openapi.domain.ao.mixLLM.MixLLMResult;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author 13225
 * @date 2025/11/10 18:05
 * LLM结果管理者
 */
@Slf4j
public class MixLLMManager {

    @NonNull
    public static List<MixLLMResult> parseResult(String result){
        List<MixLLMResult> mixLLMResults = new ArrayList<>();
        try {
            JSONArray jsonArray = JSON.parseArray(result);

            for (int i = 0; i < jsonArray.size(); i++) {
                MixLLMResult mixLLMResult = jsonArray.getObject(i, MixLLMResult.class);
                mixLLMResults.add(mixLLMResult);
            }
        } catch (Exception e){
            log.error("[MixLLMManager] 解析mix LLM result异常", e);
        }

        return mixLLMResults;
    }

    private final Queue<MixLLMResult> mixLLMResults = new ConcurrentLinkedQueue<>();



    public void start(String result) {
        mixLLMResults.clear();
        List<MixLLMResult> mixLLMResults = parseResult(result);
        StringBuilder sb = new StringBuilder();
        for (MixLLMResult mixLLMResult : mixLLMResults){
        }

        // 提取 + 合并出来sentence -> tts
        // tts -> audio -> text + {audio, event}... -> client
    }

}
