package com.openapi.service.impl.model;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.openapi.config.ChatConfig;
import com.openapi.domain.constant.ModelConstant;
import com.openapi.interfaces.mixLLM.STTCallback;
import com.openapi.service.model.STTServiceService;
import io.reactivex.Flowable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;

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
            Flowable<ByteBuffer> audioSource,
            STTCallback sttCallback
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
                                         STTCallback sttCallback) {
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

}
