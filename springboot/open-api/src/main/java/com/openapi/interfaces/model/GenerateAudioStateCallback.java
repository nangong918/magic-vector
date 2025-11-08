package com.openapi.interfaces.model;

import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import org.reactivestreams.Subscription;

/**
 * @author 13225
 * @date 2025/11/8 10:06
 */
public interface GenerateAudioStateCallback {
    void onSubscribe(Subscription subscription);
    void onFinish() throws NoApiKeyException, UploadFileException;
    void onNext(String audioBase64Data);
    void haveNoSentence();
    void onError(Throwable throwable);
}
