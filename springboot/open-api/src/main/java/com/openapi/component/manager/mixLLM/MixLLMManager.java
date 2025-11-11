package com.openapi.component.manager.mixLLM;

import com.openapi.domain.ao.mixLLM.MixLLMAudio;
import com.openapi.domain.ao.mixLLM.MixLLMResult;
import com.openapi.interfaces.mixLLM.LLMCallback;
import com.openapi.interfaces.mixLLM.TTSCallback;
import io.reactivex.disposables.Disposable;
import lombok.extern.slf4j.Slf4j;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author 13225
 * @date 2025/11/10 18:05
 * LLM结果管理者
 */
@Slf4j
public class MixLLMManager extends AbstractMixLLMManager{

    // 缓存结果(不能放在抽象类，不能多个对象用一个单例)
    protected final Queue<MixLLMResult> mixLLMResults = new ConcurrentLinkedQueue<>();
    protected final StringBuilder sentenceBuilder = new StringBuilder();

    @Override
    protected Queue<MixLLMResult> getMixLLMResultsQueue() {
        return mixLLMResults;
    }

    @Override
    protected StringBuilder getSentenceBuilder() {
        return sentenceBuilder;
    }

    public TTSCallback getDefaultTTSCallback(){
        return new TTSCallback() {
            @Override
            public void onSubscribeDisposable(Disposable disposable) {
                // 记录disposable 到 contextManager
                log.info("[MixLLMManager] tts start");
            }

            @Override
            public void onNext(MixLLMAudio mixLLMAudio) {
                // 流式音频输出
                log.info("[MixLLMManager] tts onNext, audioLength: {}, events: {}",
                        mixLLMAudio.base64Audio.length(), mixLLMAudio.eventList);
            }

            @Override
            public void onComplete() {
                // 发送TTS_END
                log.info("[MixLLMManager] tts end, 发送TTS_END");
            }

            @Override
            public void onError(Throwable throwable) {
                // 输出日志 + endConversation
                log.error("[MixLLMManager] tts error", throwable);
            }
        };
    }

    public LLMCallback getDefaultLLMCallback(){
        return new LLMCallback() {
            @Override
            public void handleResult(String result) {
                log.info("[MixLLMManager] llm result: {}", result);
            }
        };
    }
}
