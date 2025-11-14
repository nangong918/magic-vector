package com.openapi.component.manager.realTimeChat;

import com.openapi.domain.constant.realtime.RealTimeChatState;
import com.openapi.domain.entity.STTContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 13225
 * @date 2025/11/7 11:51
 * 设计模式：代理模式
 * 管理：普通对话模式和functionCall模式
 * @see SimpleLLMContext
 * @see FunctionCallLLMContext
 */
@Slf4j
public class LLMProxyContext implements RealtimeProcess, ChatRealtimeState{
    ///===========会话状态===========
    @Setter
    private RealTimeChatStateCallback stateCallback;
    private void setState(RealTimeChatState realTimeChatState){
        this.realTimeChatState = realTimeChatState;
        if (stateCallback != null){
            stateCallback.onStateChange(realTimeChatState);
        }
    }
    @Getter
    private RealTimeChatState realTimeChatState = RealTimeChatState.UNCONVERSATION;

    ///===========流程状态

    /**
     * 录音开始
     */
    @Override
    public void startRecord(){
        sttRecordContext.startRecord();
        setState(RealTimeChatState.RECORDING);
    }

    /**
     * 录音结束
     */
    @Override
    public void stopRecord(){
        sttRecordContext.stopRecord();
        setState(RealTimeChatState.WAITING_AGENT_REPLY);
    }

    /**
     * 开始LLM
     */
    @Override
    public void startLLM(){
        simpleLLMContext.getLlmContext().setLLMing(true);
        setState(RealTimeChatState.AGENT_REPLYING);
    }

    /**
     * 结束LLM
     */
    @Override
    public void stopLLM(){
        simpleLLMContext.getLlmContext().setLLMing(false);
        // 此处不设置，因为LLM和TTS存在并发状态情况，两者被归纳为了AGENT_REPLYING，所以AGENT_REPLYING由TTS结束
    }

    /**
     * 开始TTS
     * 注意FunctionCall的话需要调用setIsFinalResultTTS
     */
    @Override
    public void startTTS(){
        simpleLLMContext.getTtsContext().setTTSing(true);
        setState(RealTimeChatState.AGENT_REPLYING);
    }

    /**
     * 结束TTS
     */
    @Override
    public void stopTTS(){
        simpleLLMContext.getTtsContext().setTTSing(false);
        // 按理来说此处一定会调用endConversation()结束会话，但是这样就职责不单一，功能耦合了，还是上游去自己调用吧
        setState(RealTimeChatState.CONVERSATION_END);
    }

    /**
     * 会话结束
     */
    @Override
    public void endConversation(){
        simpleLLMContext.reset();
        sttRecordContext.reset();
        vlContext.resetAll();
        // 回调发送EOF
        setState(RealTimeChatState.CONVERSATION_END);
    }

    @Override
    public int getLLMErrorCount() {
        int count = 0;
        count = simpleLLMContext.getLlmContext().getLlmConnectResetRetryCount();
        return count;
    }

    @Override
    public boolean isLLMing() {
        return simpleLLMContext.getLlmContext().isLLMing();
    }

    @Override
    public boolean isAudioChat() {
        return sttRecordContext.isAudioChat();
    }
    @Override
    public boolean isRecording() {
        return sttRecordContext.isRecording();
    }

    /**
     * 是否是首次TTS
     * @return  true/false
     */
    @Override
    public AtomicBoolean isFirstTTS(){
        return simpleLLMContext.getTtsContext().isFirstTTS();
    }
    @Override
    public boolean isTTSing() {
        return simpleLLMContext.getTtsContext().isTTSing();
    }

    @Override
    public long getTTSStartTime() {
        return simpleLLMContext.getTtsContext().getLastTTS();
    }

    @Override
    public void setTTSStartTime(long time) {
        simpleLLMContext.getTtsContext().setLastTTS(time);
    }

    ///===========其他状态

    /**
     * 设置是否为首次TTS
     */
    @Override
    public void setIsFirstTTS(boolean isFirstTTS){
        simpleLLMContext.getTtsContext().setFirstTTS(isFirstTTS);
    }

    /**
     * 重置状态
     */
    @Override
    public void reset() {
        simpleLLMContext.reset();
        sttRecordContext.reset();
        setState(RealTimeChatState.UNCONVERSATION);
    }
    public void resetFunctionCall(){
        sttRecordContext.reset();
        setState(RealTimeChatState.UNCONVERSATION);
    }
    public void resetSimpleLLM(){
        simpleLLMContext.reset();
        sttRecordContext.reset();
        setState(RealTimeChatState.UNCONVERSATION);
    }
    ///===========会话Context===========
    // llm + tts
    private final SimpleLLMContext simpleLLMContext = new SimpleLLMContext();
    // stt
    @Getter
    private final STTContext sttRecordContext = new STTContext();
    // vl
    @Getter
    private final VLContext vlContext = new VLContext();

    /**
     * 添加TTS sentence 到缓冲池
     * @param ttsSentence ttsSentence
     */
    public void offerTTS(String ttsSentence){
        simpleLLMContext.offerTTS(ttsSentence);
    }

    /**
     * 获取所有TTS sentence
     * @return  TTS sentence
     */
    @NotNull
    public String getAllTTS() {
        return simpleLLMContext.getAllTTS();
    }


    public int getAllTTSCount(){
        return simpleLLMContext.getAllTTSCount();
    }


    public AtomicInteger getLLMConnectResetRetryCount() {
        return simpleLLMContext.getLlmContext().getLlmConnectResetRetryCountAtomic();
    }
}
