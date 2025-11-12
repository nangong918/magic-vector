package com.openapi.service.model;

import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.openapi.interfaces.model.StreamCallErrorCallback;
import com.openapi.interfaces.model.TTSStateCallback;
import com.openapi.interfaces.model.GetCurrentTTSState;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

/**
 * @author 13225
 * @date 2025/11/8 9:57
 */
public interface TTSServiceService {
    void ttsStreamCall(
            @NonNull String sentence,
            @NotNull TTSStateCallback callback
    );

    // self stream生成音频
    void ttsSafelyStreamCall(
            @NonNull String sentence,
            @NotNull TTSStateCallback callback
    );

    void ttsSafelyStreamCallErrorProxy(
            @NonNull String sentence,
            @NotNull TTSStateCallback callback,
            @NonNull StreamCallErrorCallback errorCallback
    );

    io.reactivex.disposables.Disposable generateAudio(
            @NonNull String sentence,
            @NotNull TTSStateCallback callback
    ) throws NoApiKeyException, UploadFileException;

    io.reactivex.disposables.Disposable generateAudioContinue(
            @NonNull String sentence,
            @NotNull TTSStateCallback callback,
            GetCurrentTTSState getCurrentTTSState
    ) throws NoApiKeyException, UploadFileException;
}
