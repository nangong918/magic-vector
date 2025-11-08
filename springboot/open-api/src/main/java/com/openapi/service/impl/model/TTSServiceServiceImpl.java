package com.openapi.service.impl.model;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
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
import org.springframework.stereotype.Service;

import java.util.Base64;

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
            @NonNull GenerateAudioStateCallback callback
    ) throws NoApiKeyException, UploadFileException{
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
                .doOnSubscribe(callback::onSubscribe)
                .doFinally(callback::onFinish)
                .subscribe(
                        mr -> {
                            String base64Data = mr.getOutput().getAudio().getData();
                            if (base64Data != null && !base64Data.isEmpty()){
                                byte[] audioBytes = Base64.getDecoder().decode(base64Data);
                                callback.onNext(audioBytes);
                            }
                        },
                        callback::onError
                );
    }

}
