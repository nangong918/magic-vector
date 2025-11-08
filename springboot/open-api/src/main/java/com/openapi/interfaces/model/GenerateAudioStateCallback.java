package com.openapi.interfaces.model;

import org.reactivestreams.Subscription;

/**
 * @author 13225
 * @date 2025/11/8 10:06
 */
public interface GenerateAudioStateCallback {
    void onSubscribe(Subscription subscription);
    void onFinish();
    void onNext(byte[] audioBytes);
    void onError(Throwable throwable);
}
