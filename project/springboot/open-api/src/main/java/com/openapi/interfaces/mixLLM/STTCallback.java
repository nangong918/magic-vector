package com.openapi.interfaces.mixLLM;

/**
 * @author 13225
 * @date 2025/11/12 13:42
 */
public interface STTCallback {
    /**
     * 开始
     */
    void onSTTStart();
    /**
     * 识别中
     * @param intermediateResult    中间结果
     */
    void onIdentifying(String intermediateResult);
    /**
     * 识别句子结束
     * @param sentence      句子结果
     */
    void onRecognitionSentence(String sentence);
    /**
     * 识别完成
     */
    void onRecognitionComplete();
    /**
     * 句子识别错误
     * @param e     错误信息
     */
    void onRecognitionError(Throwable e);
    /**
     * STT调用错误
     * @param e     错误信息
     */
    void onSTTError(Throwable e);
    /**
     * STT处理任务激励
     * @param disposable    处理任务
     */
    void recordDisposable(io.reactivex.disposables.Disposable disposable);
}
