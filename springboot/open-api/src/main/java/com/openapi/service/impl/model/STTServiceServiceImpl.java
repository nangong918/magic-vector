package com.openapi.service.impl.model;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.openapi.config.ChatConfig;
import com.openapi.domain.constant.ModelConstant;
import com.openapi.interfaces.mixLLM.STTCallback;
import com.openapi.interfaces.model.StreamCallErrorCallback;
import com.openapi.service.model.STTServiceService;
import io.netty.channel.ConnectTimeoutException;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;

/**
 * @author 13225
 * @date 2025/11/12 13:22
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class STTServiceServiceImpl implements STTServiceService {

    private final Recognition sttRecognizer;
    private final ChatConfig chatConfig;

    @Override
    public void sttStreamCall(
            @NonNull Flowable<ByteBuffer> audioSource,
            @NonNull STTCallback sttCallback
    ) {
        RecognitionParam sttParam = RecognitionParam.builder()
                .model(ModelConstant.STT_Model)
                .format(ModelConstant.STT_Format)
                .sampleRate(ModelConstant.SST_SampleRate)
                .apiKey(chatConfig.getApiKey())
                .build();

        try {
            var disposable = sttRecognizer.streamCall(sttParam, audioSource)
                    .doOnSubscribe(subscription -> sttCallback.onSTTStart())
                    .doFinally(sttCallback::onRecognitionComplete)
                    .subscribe(
                            recognitionResult -> handleRecognitionResult(recognitionResult, sttCallback),
                            sttCallback::onSTTError
                    );
            sttCallback.recordDisposable(disposable);
        } catch (NoApiKeyException e) {
            sttCallback.onSTTError(e);
        }
    }

    private void handleRecognitionResult(RecognitionResult result,
                                         @NonNull STTCallback sttCallback) {
        if (result == null) {
            return;
        }

        try {
            if (result.getSentence() != null && result.getSentence().getText() != null) {
                String text = result.getSentence().getText();

                if (result.isSentenceEnd()) {
                    // 获取单句最终结果
                    sttCallback.onRecognitionSentence(text);
                }
                else {
                    sttCallback.onIdentifying(text);
                }
            }
        } catch (Exception e) {
            System.err.println("处理识别结果时出错: " + e.getMessage());
        }
    }

    @Override
    public void sttStreamCallErrorProxy(
            @NonNull Flowable<ByteBuffer> audioSource,
            @NonNull STTCallback sttCallback,
            @NonNull StreamCallErrorCallback errorCallback
    ){
        STTCallback newSttCallback = new STTCallback() {
            @Override
            public void onSTTStart() {
                sttCallback.onSTTStart();
            }

            @Override
            public void onIdentifying(String intermediateResult) {
                sttCallback.onIdentifying(intermediateResult);
            }

            @Override
            public void onRecognitionSentence(String sentence) {
                sttCallback.onRecognitionSentence(sentence);
            }

            @Override
            public void onRecognitionComplete() {
                sttCallback.onRecognitionComplete();
            }

            @Override
            public void onRecognitionError(Throwable e) {
                sttCallback.onRecognitionError(e);
            }

            @Override
            public void onSTTError(Throwable e) {
                handleConnectResetTimeoutError(
                        e,
                        audioSource,
                        sttCallback,
                        errorCallback
                );
            }

            @Override
            public void recordDisposable(Disposable disposable) {
                errorCallback.addTask(disposable);
            }
        };
        sttStreamCall(audioSource, newSttCallback);
    }

    private void handleConnectResetTimeoutError(
            @NonNull Throwable e,
            @NonNull Flowable<ByteBuffer> audioSource,
            @NonNull STTCallback sttCallback,
            @NonNull StreamCallErrorCallback errorCallback)  {
        if (e instanceof WebClientRequestException || e instanceof TimeoutException || e instanceof ConnectTimeoutException) {
            log.error("[STT] 连接超时异常，异常详情：", e);

            int[] retryResult = errorCallback.addCountAndCheckIsOverLimit();
            // 超出重试限制检查
            boolean isOverLimit = retryResult[0] > 0;

            if (isOverLimit) {
                log.error("[STT] 尝试重连次数已超过限制，不再尝试重连");
                errorCallback.endConversation();
                sttCallback.onSTTError(e);
            }
            else {
                log.info("[STT] 尝试重连，当前重连次数：{}", retryResult[1]);
                sttStreamCallErrorProxy(
                        audioSource,
                        sttCallback,
                        errorCallback
                );
            }
        }
        else {
            log.error("[STT] 非连接超时异常，异常详情：", e);
            sttCallback.onSTTError(e);
        }
    }

}
