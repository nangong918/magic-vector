package com.openapi.interfaces.mixLLM;

import com.openapi.domain.ao.mixLLM.MixLLMAudio;
import io.reactivex.disposables.Disposable;
import org.reactivestreams.Subscription;

public interface TTSCallback {
    void onSubscribeDisposable(Disposable disposable);
    void onStart(Subscription subscription);
    void onNext(MixLLMAudio mixLLMAudio);
    void onComplete();
    void onError(Throwable throwable);
}
