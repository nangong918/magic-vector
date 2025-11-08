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
import com.openapi.interfaces.model.GenerateAudioStateCallback;
import com.openapi.service.model.TTSServiceService;
import io.reactivex.Flowable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Optional;

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
            @Nullable GenerateAudioStateCallback callback
    ) throws NoApiKeyException, UploadFileException{
        if (sentence.isEmpty()){
            if (callback != null){
                callback.haveNoSentence();
            }
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
                .doOnSubscribe(subscription -> {
                    if (callback != null){
                        callback.onSubscribe(subscription);
                    }
                })
                .doFinally(() -> {
                    if (callback != null){
                        callback.onFinish();
                    }
                })
                .subscribe(
                        mr -> {
                            if (callback != null){
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
                            }
                        },
                        throwable -> {
                            if (callback != null){
                                callback.onError(throwable);
                            }
                        }
                );
    }

}
