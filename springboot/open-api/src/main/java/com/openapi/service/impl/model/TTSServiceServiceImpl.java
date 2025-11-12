package com.openapi.service.impl.model;

import com.alibaba.dashscope.aigc.multimodalconversation.AudioResult;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationOutput;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.openapi.config.ChatConfig;
import com.openapi.domain.constant.ModelConstant;
import com.openapi.interfaces.model.StreamCallErrorCallback;
import com.openapi.interfaces.model.TTSStateCallback;
import com.openapi.interfaces.model.GetCurrentTTSState;
import com.openapi.service.model.TTSServiceService;
import io.netty.channel.ConnectTimeoutException;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Subscription;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author 13225
 * @date 2025/11/8 9:57
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class TTSServiceServiceImpl implements TTSServiceService {

    private final MultiModalConversation multiModalConversation;
    private final ChatConfig chatConfig;

    @Override
    public void ttsStreamCall(
            @NonNull String sentence,
            @NotNull TTSStateCallback callback
    ) {
        if (sentence.isEmpty()){
            callback.haveNoSentence();
            return;
        }

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model(ModelConstant.TTS_Model)
                .apiKey(chatConfig.getApiKey())
                .text(sentence)
                .voice(ModelConstant.TTS_Voice)
                .languageType(ModelConstant.TTS_LanguageType)
                .build();

        Flowable<MultiModalConversationResult> result;

        try {
            result = multiModalConversation.streamCall(param);
        } catch (NoApiKeyException | UploadFileException e) {
            callback.onError(e);
            return;
        }

        // 结束
        var disposable = result
                .doOnSubscribe(callback::onStart)
                .doFinally(callback::onSingleComplete)
                .subscribe(
                        mr -> {
                            // 音频数据
                            Optional.ofNullable(mr)
                                    .map(MultiModalConversationResult::getOutput)
                                    .map(MultiModalConversationOutput::getAudio)
                                    .map(AudioResult::getData)
                                    .ifPresent(
                                            base64Data -> {
                                                if (!base64Data.isEmpty()){
                                                    callback.onNext(base64Data);
                                                }
                                            }
                                    );
                        },
                        callback::onError
                );
        callback.recordDisposable(disposable);
    }

    // self stream生成音频 -> 内部包含迭代器
    @Override
    public void ttsSafelyStreamCall(
            @NonNull String sentence,
            @NotNull TTSStateCallback callback
    ){
        // sentence检查
        if (sentence.isEmpty()){
            callback.haveNoSentence();
            return;
        }

        // sentence超出限制长度
        // 句子切分
        List<String> splitSentence = splitSentence(sentence);

        if (splitSentence.isEmpty()){
            callback.haveNoSentence();
            return;
        }

        // 生成splitSentence迭代器 设计模式:迭代器
        Iterator<String> splitSentenceIterator = splitSentence.iterator();

        AtomicBoolean isFirst = new AtomicBoolean(true);
        TTSStateCallback allTTSStateCallback = new TTSStateCallback() {
            @Override
            public void recordDisposable(Disposable disposable) {
                callback.recordDisposable(disposable);
            }

            @Override
            public void onStart(Subscription subscription) {
                // 检查是否是第一次启动, 并设置为false
                if (isFirst.compareAndSet(true, false)){
                    callback.onStart(subscription);
                }
            }

            @Override
            public void onNext(String audioBase64Data) {
                callback.onNext(audioBase64Data);
            }

            @Override
            public void onSingleComplete() {
                // 最后一个完成: 当完成数量是finish - 1 时, 因为在调用的时候还没有++ 所以是finish - 1
                if (splitSentenceIterator.hasNext()){
                    ttsStreamCall(
                            splitSentenceIterator.next(),
                            this
                    );
                }
                else {
                    try {
                        callback.onSingleComplete();
                    } catch (Exception e) {
                        callback.onError(e);
                    }
                }
            }

            @Override
            public void onAllComplete() {
                // 根本就不是这个接口调用的, 这个All Complete是LLM全部生成的句子都处理完了的All Complete
                // 最后一个完成: 当完成数量是finish - 1 时, 因为在调用的时候还没有++ 所以是finish - 1
                if (!splitSentenceIterator.hasNext()) {
                    try {
                        callback.onAllComplete();
                    } catch (Exception e) {
                        callback.onError(e);
                    }
                }
            }

            @Override
            public void haveNoSentence() {
                callback.haveNoSentence();
            }

            @Override
            public void onError(Throwable throwable) {
                callback.onError(throwable);
            }
        };
        ttsStreamCall(
                splitSentenceIterator.next(),
                allTTSStateCallback
        );
    }

    @Override
    public void ttsSafelyStreamCallErrorProxy(
            @NonNull String sentence,
            @NotNull TTSStateCallback callback,
            @NonNull StreamCallErrorCallback errorCallback
    ) {
        TTSStateCallback proxyCallback = new TTSStateCallback() {
            @Override
            public void recordDisposable(Disposable disposable) {
                callback.recordDisposable(disposable);
            }

            @Override
            public void onStart(Subscription subscription) {
                callback.onStart(subscription);
            }

            @Override
            public void onNext(String audioBase64Data) {
                callback.onNext(audioBase64Data);
            }

            @Override
            public void onSingleComplete() throws NoApiKeyException, UploadFileException {
                callback.onSingleComplete();
            }

            @Override
            public void onAllComplete() {
                callback.onAllComplete();
            }

            @Override
            public void haveNoSentence() {
                callback.haveNoSentence();
            }

            @Override
            public void onError(Throwable throwable) {
                handleConnectResetTimeoutError(
                        throwable,
                        sentence,
                        callback,
                        errorCallback
                );
            }
        };
        ttsSafelyStreamCall(
                sentence,
                proxyCallback
        );
    }

    private void handleConnectResetTimeoutError(
            Throwable e,
            @NonNull String sentence,
            @NonNull TTSStateCallback ttsStateCallback,
            @NonNull StreamCallErrorCallback errorCallback
    ){
        if (e instanceof WebClientRequestException || e instanceof TimeoutException || e instanceof ConnectTimeoutException){
            // 连接超时
            log.error("[ttsSafelyStreamCallErrorProxy] 连接超时: {}", sentence, e);

            int[] retryResult = errorCallback.addCountAndCheckIsOverLimit();
            // 超出重试限制检查
            boolean isOverLimit = retryResult[0] > 0;

            if (isOverLimit) {
                log.error("[ttsSafelyStreamCallErrorProxy] 尝试重连次数已超过限制，不再尝试重连");
                errorCallback.endConversation();
            }
            else {
                log.info("[ttsSafelyStreamCallErrorProxy] 尝试重连，当前重连次数：{}", retryResult[1]);

                ttsSafelyStreamCallErrorProxy(
                        sentence,
                        ttsStateCallback,
                        errorCallback
                );
            }
        }
        else {
            // 其他错误
            log.error("[ttsSafelyStreamCallErrorProxy] 错误: {}", sentence, e);
            ttsStateCallback.onError(e);
        }
    }

    @NotNull
    private static List<String> splitSentence(@NotNull String sentence) {
        List<String> splitSentence = new LinkedList<>();
        if (sentence.length() > ModelConstant.TTS_MaxLength){
            // 讲句子拆分为ModelConstant.TTS_MaxLength等分
            for (int i = 0; i < sentence.length(); i += ModelConstant.TTS_MaxLength){
                splitSentence.add(
                        sentence.substring(i, Math.min(
                                i + ModelConstant.TTS_MaxLength, sentence.length()
                        ))
                );
            }
        }
        else {
            splitSentence.add(sentence);
        }
        log.info("[splitSentence] 句子拆分 splitSentence size: {}", splitSentence.size());
        return splitSentence;
    }

    /**
     * 生成音频
     * @param sentence              待转换的句子
     * @param callback              回调
     * @return                      线程订阅管理
     * @throws NoApiKeyException    无API Key
     * @throws UploadFileException  文件上传异常
     */
    @Override
    public io.reactivex.disposables.Disposable generateAudio(
            @NonNull String sentence,
            @NotNull TTSStateCallback callback
    ) throws NoApiKeyException, UploadFileException{

        if (sentence.isEmpty()){
            callback.haveNoSentence();
            return null;
        }

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model(ModelConstant.TTS_Model)
                .apiKey(chatConfig.getApiKey())
                .text(sentence)
                .voice(ModelConstant.TTS_Voice)
                .languageType(ModelConstant.TTS_LanguageType)
                .build();

        Flowable<MultiModalConversationResult> result;

        result = multiModalConversation.streamCall(param);

        // 结束
        return result
                .doOnSubscribe(callback::onStart)
                .doFinally(callback::onSingleComplete)
                .subscribe(
                        mr -> {
                            // 音频数据
                            Optional.ofNullable(mr)
                                    .map(MultiModalConversationResult::getOutput)
                                    .map(MultiModalConversationOutput::getAudio)
                                    .map(AudioResult::getData)
                                    .ifPresent(
                                            base64Data -> {
                                                if (!base64Data.isEmpty()){
                                                    callback.onNext(base64Data);
                                                }
                                            }
                                    );
                        },
                        callback::onError
                );
    }

    /**
     * 继续生成音频
     * @param sentence                  待转换的句子
     * @param callback                  回调
     * @return                          线程订阅管理
     * @throws NoApiKeyException        无API Key
     * @throws UploadFileException      文件上传异常
     */
    @Override
    public io.reactivex.disposables.Disposable generateAudioContinue(
            @NonNull String sentence,
            @NotNull TTSStateCallback callback,
            GetCurrentTTSState getCurrentTTSState
    ) throws NoApiKeyException, UploadFileException {

        if (sentence.isEmpty()){
            callback.haveNoSentence();
            return null;
        }

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model(ModelConstant.TTS_Model)
                .apiKey(chatConfig.getApiKey())
                .text(sentence)
                .voice(ModelConstant.TTS_Voice)
                .languageType(ModelConstant.TTS_LanguageType)
                .build();

        Flowable<MultiModalConversationResult> result;

        result = multiModalConversation.streamCall(param);

        return result
                .doOnSubscribe(subscription -> log.info("[generateAudioContinue] 开始生成音频，无回调"))
                .doFinally(() -> {
                    callback.onSingleComplete();
                    boolean isFinallyFinish = getCurrentTTSState.getIsFinallyFinish();
                    log.info("[generateAudioContinue] 音频生成完毕，回调: {}", isFinallyFinish);
                    if (isFinallyFinish) {
                        callback.onAllComplete();
                    }
                })
                .subscribe(
                        mr -> {
                            // 音频数据
                            Optional.ofNullable(mr)
                                    .map(MultiModalConversationResult::getOutput)
                                    .map(MultiModalConversationOutput::getAudio)
                                    .map(AudioResult::getData)
                                    .ifPresent(
                                            base64Data -> {
                                                if (!base64Data.isEmpty()){
                                                    callback.onNext(base64Data);
                                                }
                                            }
                                    );
                        },
                        callback::onError
                );
    }

}
