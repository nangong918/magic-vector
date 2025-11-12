package com.openapi.interfaces.model;

import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import io.reactivex.disposables.Disposable;
import org.reactivestreams.Subscription;

/**
 * @author 13225
 * @date 2025/11/8 10:06
 * TTS 的状态回调
 */
public interface TTSStateCallback {
    void recordDisposable(Disposable disposable);
    void onStart(Subscription subscription);
    void onNext(String audioBase64Data);
    void onSingleComplete() throws NoApiKeyException, UploadFileException;
    void onAllComplete();
    void haveNoSentence();
    void onError(Throwable throwable);
}
