package com.openapi.service.model;

import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.openapi.interfaces.model.GenerateAudioStateCallback;
import lombok.NonNull;

/**
 * @author 13225
 * @date 2025/11/8 9:57
 */
public interface TTSServiceService {
    io.reactivex.disposables.Disposable generateAudio(
            @NonNull String sentence,
            @NonNull GenerateAudioStateCallback callback
    ) throws NoApiKeyException, UploadFileException;
}
