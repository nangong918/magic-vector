package com.openapi.component.manager.mixLLM;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.openapi.domain.ao.mixLLM.MixLLMAudio;
import com.openapi.domain.ao.mixLLM.MixLLMResult;
import com.openapi.interfaces.mixLLM.TTSCallback;
import com.openapi.interfaces.model.GenerateAudioStateCallback;
import com.openapi.service.model.TTSServiceService;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import org.springframework.util.CollectionUtils;

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

    // todo 测试
    public void start(String result, TTSServiceService ttsServiceService) {

        // 提取 + 合并出来sentence -> tts
        mixLLMResults.clear();
        List<MixLLMResult> results = parseResult(result);
        StringBuilder sb = new StringBuilder();
        for (MixLLMResult mixLLMResult : results){
            if (CollectionUtils.isEmpty(mixLLMResult.eventList)){
                sb.append(mixLLMResult.chatSentence);
            }
            else {
                if (!sb.isEmpty()){
                    MixLLMResult lastMixLLMResult = new MixLLMResult();
                    lastMixLLMResult.chatSentence = sb.toString();
                    mixLLMResults.offer(lastMixLLMResult);
                    sb.setLength(0);
                }
                mixLLMResults.offer(mixLLMResult);
            }
        }
        if (!sb.isEmpty()){
            MixLLMResult lastMixLLMResult = new MixLLMResult();
            lastMixLLMResult.chatSentence = sb.toString();
            mixLLMResults.offer(lastMixLLMResult);
            sb.setLength(0);
        }

        // tts -> audio -> text + {audio, event}... -> client
        tts(ttsServiceService);
    }

    // 定义callback
    private void tts(TTSServiceService ttsServiceService){
        TTSCallback ttsCallback = new TTSCallback() {
            @Override
            public void onSubscribeDisposable(Disposable disposable) {
                // 记录disposable 到 contextManager
            }

            @Override
            public void onNext(MixLLMAudio mixLLMAudio) {
                // 流式音频输出
            }

            @Override
            public void onComplete() {
                // 发送TTS_END
            }

            @Override
            public void onError(Throwable throwable) {
                // 输出日志 + endConversation
            }
        };

        ttsQueueStream(ttsServiceService, ttsCallback);
    }

    // Queue(sentence + eventList) -> callback(streamAudio)
    private void ttsQueueStream(
            TTSServiceService ttsServiceService,
            TTSCallback ttsCallback
    ){
        if (mixLLMResults.isEmpty()){
            ttsCallback.onComplete();
            return;
        }
        MixLLMResult mixLLMResult = mixLLMResults.poll();
        Flowable<MixLLMAudio> ttsFlowable = ttsStream(ttsServiceService, mixLLMResult);
        var disposable = ttsFlowable.doOnSubscribe(subscription -> {})
                .doFinally(() -> {
                    if (!mixLLMResults.isEmpty()) {
                        ttsQueueStream(ttsServiceService, ttsCallback);
                    }
                    else {
                        ttsCallback.onComplete();
                    }
                })
                .subscribe(
                        ttsCallback::onNext,
                        ttsCallback::onError
                );
        ttsCallback.onSubscribeDisposable(disposable);
    }

    // (sentence + eventList) -> streamAudio
    private Flowable<MixLLMAudio> ttsStream(
            TTSServiceService ttsServiceService,
            MixLLMResult mixLLMResult) {
        return Flowable.create(fluxSink -> {
            try {
                ttsServiceService.generateAudio(
                        mixLLMResult.chatSentence,
                        new GenerateAudioStateCallback() {
                            boolean isFirst = true;
                            @Override
                            public void onSubscribe(Subscription subscription) {

                            }

                            @Override
                            public void onSingleFinish() {
                                // 单个音频生成完成
                                fluxSink.onComplete();
                            }

                            @Override
                            public void onAllFinish() {
                                // 不用管，这个结束不是为这个方法设计的
                            }

                            @Override
                            public void onNext(String audioBase64Data) {
                                MixLLMAudio mixLLMAudio = new MixLLMAudio();
                                if (isFirst){
                                    isFirst = false;
                                    mixLLMAudio.eventList = mixLLMResult.eventList;
                                }
                                mixLLMAudio.base64Audio = audioBase64Data;

                                // 发送数据到Flux
                                if (!fluxSink.isCancelled()) {
                                    fluxSink.onNext(mixLLMAudio);
                                }
                            }

                            @Override
                            public void haveNoSentence() {
                                // 没有句子时直接完成
                                if (!fluxSink.isCancelled()) {
                                    fluxSink.onComplete();
                                }
                            }

                            @Override
                            public void onError(Throwable throwable) {
                                // 发送错误
                                if (!fluxSink.isCancelled()) {
                                    fluxSink.onError(throwable);
                                }
                            }
                        }
                );
            } catch (Exception e){
                // 发送错误
                if (!fluxSink.isCancelled()) {
                    fluxSink.onError(e);
                }
            }
        }, BackpressureStrategy.BUFFER);
    }
}
