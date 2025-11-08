package com.openapi.interfaces.model;

import org.reactivestreams.Subscription;
import reactor.core.publisher.SignalType;

/**
 * @author 13225
 * @date 2025/11/8 14:29
 * LLM 的状态回调
 */
public interface LLMStateCallback {
    void onSubscribe(Subscription subscription);
    void onFinish(SignalType signalType);
    void onNext(String fragment);
    void haveNoSentence();
    void onError(Throwable throwable);
}
