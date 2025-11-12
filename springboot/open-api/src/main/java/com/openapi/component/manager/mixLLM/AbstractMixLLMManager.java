package com.openapi.component.manager.mixLLM;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.openapi.domain.ao.mixLLM.MixLLMAudio;
import com.openapi.domain.ao.mixLLM.MixLLMResult;
import com.openapi.interfaces.mixLLM.LLMCallback;
import com.openapi.interfaces.mixLLM.TTSCallback;
import com.openapi.interfaces.model.TTSStateCallback;
import com.openapi.service.model.TTSServiceService;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * @author 13225
 * @date 2025/11/11 10:22
 * 设计模式：抽象方法模板
 */
@Slf4j
public abstract class AbstractMixLLMManager {

    // 固定不变的模板方法 - final
    public final List<MixLLMResult> parseResult(String result) throws JSONException {
        List<MixLLMResult> mixLLMResults = new ArrayList<>();

        JSONArray jsonArray = JSON.parseArray(result);

        for (int i = 0; i < jsonArray.size(); i++) {
            MixLLMResult mixLLMResult = jsonArray.getObject(i, MixLLMResult.class);
            mixLLMResults.add(mixLLMResult);
        }

        return mixLLMResults;
    }

    // Queue(sentence + eventList) -> callback(streamAudio)
    protected final void ttsQueueStream(
            Queue<MixLLMResult> mixLLMResults,
            TTSServiceService ttsServiceService,
            TTSCallback ttsCallback
    ) {
        if (mixLLMResults.isEmpty()) {
            ttsCallback.onComplete();
            return;
        }
        MixLLMResult mixLLMResult = mixLLMResults.poll();
        Flowable<MixLLMAudio> ttsFlowable = ttsStream(ttsServiceService, mixLLMResult);
        var disposable = ttsFlowable.doOnSubscribe(ttsCallback::onStart)
                .doFinally(() -> {
                    if (!mixLLMResults.isEmpty()) {
                        ttsQueueStream(mixLLMResults, ttsServiceService, ttsCallback);
                    } else {
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
    protected final Flowable<MixLLMAudio> ttsStream(
            TTSServiceService ttsServiceService,
            MixLLMResult mixLLMResult) {
        return Flowable.create(fluxSink -> {
            try {
                ttsServiceService.generateAudio(
                        mixLLMResult.chatSentence,
                        new TTSStateCallback() {
                            boolean isFirst = true;

                            @Override
                            public void recordDisposable(Disposable disposable) {

                            }

                            @Override
                            public void onStart(Subscription subscription) {

                            }

                            @Override
                            public void onSingleComplete() {
                                // 单个音频生成完成
                                fluxSink.onComplete();
                            }

                            @Override
                            public void onAllComplete() {
                                // 不是此方法应该关心的；不实现
                            }

                            @Override
                            public void onNext(String audioBase64Data) {
                                MixLLMAudio mixLLMAudio = new MixLLMAudio();
                                if (isFirst) {
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
            } catch (Exception e) {
                // 发送错误
                if (!fluxSink.isCancelled()) {
                    fluxSink.onError(e);
                }
            }
        }, BackpressureStrategy.BUFFER);
    }

    // 将start方法改为final模板方法
    public final void start(String result, TTSServiceService ttsServiceService, TTSCallback ttsCallback, LLMCallback llmCallback) {
        // 步骤1: 解析结果
        List<MixLLMResult> results = parseResult(result);

        // 步骤2: 合并句子和事件
        mergeSentencesAndEvents(results);

        // 步骤3: 将提取到的句子结果交付出去
        llmCallback.handleResult(getSentenceBuilder().toString());

        // 步骤4: 开始TTS流处理
        ttsQueueStream(getMixLLMResultsQueue(), ttsServiceService, ttsCallback);
    }


    // 固定不变的合并逻辑 - final
    protected final void mergeSentencesAndEvents(List<MixLLMResult> results) {
        Queue<MixLLMResult> queue = getMixLLMResultsQueue();
        queue.clear();
        StringBuilder sentenceBuilder = getSentenceBuilder();
        sentenceBuilder.setLength(0);
        StringBuilder sb = new StringBuilder();

        for (MixLLMResult mixLLMResult : results) {
            if (CollectionUtils.isEmpty(mixLLMResult.eventList)) {
                sb.append(mixLLMResult.chatSentence);
            } else {
                if (!sb.isEmpty()) {
                    MixLLMResult lastMixLLMResult = createMixLLMResult(sb.toString());
                    queue.offer(lastMixLLMResult);
                    sb.setLength(0);
                }
                mixLLMResult = processMixLLMResultWithEvents(mixLLMResult);
                queue.offer(mixLLMResult);
            }
            // 保存句子
            sentenceBuilder.append(mixLLMResult.chatSentence);
        }

        if (!sb.isEmpty()) {
            MixLLMResult lastMixLLMResult = createMixLLMResult(sb.toString());
            queue.offer(lastMixLLMResult);
            sb.setLength(0);
        }
    }


    // 抽象方法 - 子类提供具体的队列实现
    protected abstract Queue<MixLLMResult> getMixLLMResultsQueue();
    protected abstract StringBuilder getSentenceBuilder();
    public abstract void reset();

    protected MixLLMResult createMixLLMResult(String sentence) {
        MixLLMResult result = new MixLLMResult();
        result.chatSentence = sentence;
        return result;
    }

    protected MixLLMResult processMixLLMResultWithEvents(MixLLMResult mixLLMResult) {
        // 默认直接返回，子类可以重写来处理带有事件的MixLLMResult
        return mixLLMResult;
    }
}
