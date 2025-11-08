package com.openapi.service.model;

import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.openapi.interfaces.model.GenerateAudioStateCallback;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author 13225
 * @date 2025/11/8 9:57
 */
public interface TTSServiceService {
    io.reactivex.disposables.Disposable generateAudio(
            @NonNull String sentence,
            @NotNull GenerateAudioStateCallback callback
    ) throws NoApiKeyException, UploadFileException;

    io.reactivex.disposables.Disposable generateAudioContinue(
            @NonNull String sentence,
            @NotNull GenerateAudioStateCallback callback,
            boolean isFinallyFinish
    ) throws NoApiKeyException, UploadFileException;
}
