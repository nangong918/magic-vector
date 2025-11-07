package com.openapi.domain.entity;

import com.openapi.domain.constant.realtime.RealTimeChatStatue;
import com.openapi.domain.entity.realtimeChat.STTContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author 13225
 * @date 2025/11/7 11:51
 * 设计模式：代理模式
 * 管理：普通对话模式和functionCall模式
 * @see SimpleLLMContext
 * @see FunctionCallLLMContext
 */
@Slf4j
public class LLMProxyContext {
    ///===========会话类型===========
    // 是否是FunctionCall会话
    private final AtomicBoolean isFunctionCall = new AtomicBoolean(false);
    public void addFunctionCallRecord(){
        isFunctionCall.set(true);
        functionCallLLMContext.addFunctionCallSignal();
    }
    public boolean isFunctionCall() {
        return isFunctionCall.get();
    }
    public void setFunctionCallFinishCallBack(AllFunctionCallFinished allFunctionCallFinished){
        this.functionCallLLMContext.allFunctionCallFinished = allFunctionCallFinished;
    }

    ///===========会话状态===========
    @Setter
    private RealTimeChatStatueCallback statueCallback;
    private void setStatue(RealTimeChatStatue realTimeChatStatue){
        this.realTimeChatStatue = realTimeChatStatue;
        if (statueCallback != null){
            statueCallback.onStatueChange(realTimeChatStatue);
        }
    }
    @Getter
    private RealTimeChatStatue realTimeChatStatue = RealTimeChatStatue.UNCONVERSATION;

    ///===========流程状态

    /**
     * 录音开始
     */
    public void startRecord(){
        sstRecordContext.startRecord();
        setStatue(RealTimeChatStatue.RECORDING);
    }

    /**
     * 录音结束
     */
    public void stopRecord(){
        sstRecordContext.stopRecord();
        setStatue(RealTimeChatStatue.WAITING_AGENT_REPLY);
    }

    /**
     * 开始LLM
     */
    public void startLLM(){
        if (isFunctionCall.get()){
            functionCallLLMContext.getLlmContext().setLLMing(true);
        }
        else {
            simpleLLMContext.getLlmContext().setLLMing(true);
        }
        setStatue(RealTimeChatStatue.AGENT_REPLYING);
    }

    /**
     * 结束LLM
     */
    public void stopLLM(){
        if (isFunctionCall.get()){
            functionCallLLMContext.getLlmContext().setLLMing(false);
        }
        else {
            simpleLLMContext.getLlmContext().setLLMing(false);
        }
        // 此处不设置，因为LLM和TTS存在并发状态情况，两者被归纳为了AGENT_REPLYING，所以AGENT_REPLYING由TTS结束
    }

    /**
     * 开始TTS
     * 注意FunctionCall的话需要调用setIsFinalResultTTS
     */
    public void startTTS(){
        if (isFunctionCall.get()){
            functionCallLLMContext.getTtsContext().setTTSing(true);
        }
        else {
            simpleLLMContext.getTtsContext().setTTSing(true);
        }
        setStatue(RealTimeChatStatue.AGENT_REPLYING);
    }

    /**
     * 结束TTS
     */
    public void stopTTS(){
        if (isFunctionCall.get()){
            functionCallLLMContext.getTtsContext().setTTSing(false);
            // 如果是最终的tts
            // 检查是否可以执行最后result的LLM；检查点2: 最终的TTS结束 -> 对应tts比function call慢
            if (functionCallLLMContext.isFinalResultTTS()){
                var remainingFunctionCallSignal = functionCallLLMContext.getRemainingFunctionCallSignal();
                if (remainingFunctionCallSignal > 0){
                    log.info("[FunctionCall] Final TTS 执行完毕FunctionCall结果，剩余function call信号量：{}", remainingFunctionCallSignal);
                }
                else {
                    log.info("[FunctionCall] 检查点2 所有FunctionCall结果获取完毕，开始执行最后result的LLM");
                    functionCallLLMContext.allFunctionCallFinished.allFunctionCallFinished();
                }
            }
        }
        else {
            simpleLLMContext.getTtsContext().setTTSing(false);
            // 按理来说此处一定会调用endConversation()结束会话，但是这样就职责不单一，功能耦合了，还是上游去自己调用吧
        }
        setStatue(RealTimeChatStatue.CONVERSATION_END);
    }

    /**
     * 会话结束
     */
    public void endConversation(){
        simpleLLMContext.reset();
        functionCallLLMContext.reset();
        sstRecordContext.reset();
        // 回调发送EOF
        setStatue(RealTimeChatStatue.CONVERSATION_END);
    }

    ///===========其他状态

    /**
     * 设置是否为首次TTS
     */
    public void setIsFirstTTS(boolean isFirstTTS){
        if (isFunctionCall.get()){
            functionCallLLMContext.getTtsContext().setFirstTTS(isFirstTTS);
        }
        else {
            simpleLLMContext.getTtsContext().setFirstTTS(isFirstTTS);
        }
    }

    /**
     * 是否是首次TTS
     * @return  true/false
     */
    public boolean isFirstTTS(){
        if (isFunctionCall.get()){
            return functionCallLLMContext.getTtsContext().isFirstTTS();
        }
        else {
            return simpleLLMContext.getTtsContext().isFirstTTS();
        }
    }

    /**
     * 重置状态
     */
    public void reset() {
        simpleLLMContext.reset();
        functionCallLLMContext.reset();
        sstRecordContext.reset();
        setStatue(RealTimeChatStatue.UNCONVERSATION);
    }

    ///===========会话Context===========
    private final SimpleLLMContext simpleLLMContext = new SimpleLLMContext();
    private final FunctionCallLLMContext functionCallLLMContext = new FunctionCallLLMContext();
    // 音频
    // 音频状态
    @Getter
    private final STTContext sstRecordContext = new STTContext();

    /**
     * 添加TTS sentence 到缓冲池
     * @param ttsSentence ttsSentence
     */
    public void offerTTS(String ttsSentence){
        if (isFunctionCall.get()){
            functionCallLLMContext.offerTTS(ttsSentence);
        }
        else {
            simpleLLMContext.offerTTS(ttsSentence);
        }
    }

    /**
     * 获取所有TTS sentence
     * @return  TTS sentence
     */
    public String getAllTTS() {
        if (isFunctionCall.get()){
            return functionCallLLMContext.getAllTTS();
        }
        else {
            return simpleLLMContext.getAllTTS();
        }
    }


}
