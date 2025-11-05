package com.openapi.domain.interfaces;

/**
 * @author 13225
 * @date 2025/11/5 13:15
 */
public interface OnTTSSelfCall {
    void selfCall(io.reactivex.disposables.Disposable ttsDisposable, boolean isFunctionCall);
}
