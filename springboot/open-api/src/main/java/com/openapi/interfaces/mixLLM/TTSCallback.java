package com.openapi.interfaces.mixLLM;

import com.openapi.domain.ao.mixLLM.MixLLMAudio;
import io.reactivex.disposables.Disposable;

public interface TTSCallback {
    void onSubscribeDisposable(Disposable disposable);
    void onNext(MixLLMAudio mixLLMAudio);
    void onComplete();
    void onError(Throwable throwable);
}
