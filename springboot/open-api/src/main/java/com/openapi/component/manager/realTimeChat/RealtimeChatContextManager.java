package com.openapi.component.manager.realTimeChat;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.openapi.component.manager.mixLLM.MixLLMManager;
import com.openapi.domain.ao.mixLLM.McpSwitch;
import com.openapi.domain.constant.ModelConstant;
import com.openapi.domain.constant.RoleTypeEnum;
import com.openapi.domain.constant.realtime.RealtimeResponseDataTypeEnum;
import com.openapi.domain.dto.ws.response.RealtimeChatTextResponse;
import com.openapi.interfaces.connect.ConnectionSession;
import com.openapi.utils.DateUtils;
import com.openapi.connect.websocket.manager.PersistentConnectMessageManager;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 13225
 * @date 2025/10/16 9:50
 * 实时聊天的上下文管理器，一个连接一个，并非单例，不能使用@Component
 * 需求：1. 用户发送语音 + 回调显示用户语音 2. 流式音频回调 3. 流式文本持续回显 4. 聊天记录保存到mysql 5.下次会话的时候加载到ChatModel
 * vision chat工作流 ->
 *  userMessage -> chatClient -> functionCall(将context manager设置为functionCalling) -> response1 -> response2(取消response1的内容)
 *  response1 [结束：function calling -> 不发送EOF] response2 [开始：function calling -> 不发送起始符]
 *  任务管理：取消chat任务，取消function call任务：添加任务，取消任务，数据复位
 *  设计模式：
 *      1. 桥接模式：拆分为多个抽象接口实现
 *
 *      状态设计模式。将各个布尔值改为连贯的几个变量, 并回调对外公开内部状态 -> [UML状态图、流程图draw.io]
 *      录音状态：单独boolean，因为需要控制循环线程的结束
 *      并发任务提取出来，因为能以一个状态值表示
 *      状态：未对话，user正在讲话，agent正在回复，
 *
 *  需要升级：chat（无function call） -> chat + function call -> chat + 工作流式function call
 *  工作流模式：唯一输出llm无论再多的工作流，只有一个llm出口；
 *  eg：帮我看看我的账单，看看我最近是不是超支了：chat-> visionTool, databaseTool
 *  -> resultJson集合 + (等待完成)
 *  -> end llm
 */
@Slf4j
public class RealtimeChatContextManager implements
        IRealTimeChatResponseManager, RealtimeProcess, ChatRealtimeState,
        FunctionCallMethod {

    private final PersistentConnectMessageManager webSocketMessageManager;
    public RealtimeChatContextManager(
            @NonNull PersistentConnectMessageManager webSocketMessageManager){
        this.webSocketMessageManager = webSocketMessageManager;
    }

    public void setAllFunctionCallFinished(AllFunctionCallFinished allFunctionCallFinished){
        this.llmProxyContext.setFunctionCallFinishCallBack(allFunctionCallFinished);
    }

    /// chatClient      (chatModel是单例，但是chatClient需要集成Agent的记忆，以及每个chatClient的设定不同，所以不是单例)
    @Getter
    public ChatClient chatClient;

    /// 会话任务
    private final List<Object> chatTasks = new ArrayList<>();
    private final List<Object> functionCallTasks = new ArrayList<>();

    /// 会话状态
    public final LLMProxyContext llmProxyContext = new LLMProxyContext();

    // 添加任务
    public boolean addChatTask(Object task) {
        return addTask(task, chatTasks);
    }
    public boolean addFunctionCallTask(Object task) {
        return addTask(task, functionCallTasks);
    }
    private boolean addTask(Object task, @NotNull List<Object> tasks){
        if (task == null){
            log.warn("task is null");
            return false;
        }
        if (task instanceof io.reactivex.disposables.Disposable ||
                task instanceof reactor.core.Disposable ||
                task instanceof Future<?>){
            return tasks.add(task);
        }
        else {
            log.warn("task is not a Disposable or a Future");
        }
        return false;
    }

    // 取消任务(方法模板)
    public final void cancelChatTask(){
        cancelTask(chatTasks);
        llmProxyContext.resetFunctionCall();
    }
    public final void cancelFunctionCallTask(){
        cancelTask(functionCallTasks);
        llmProxyContext.resetSimpleLLM();
    }
    private synchronized void cancelTask(@NotNull List<Object> tasks){
        if (tasks.isEmpty()){
            return;
        }
        int cancelCount = 0;
        for (Object task: tasks){
            if (task == null){
                log.warn("task is null");
                continue;
            }
            switch (task) {
                case io.reactivex.disposables.Disposable disposable -> disposable.dispose();
                case reactor.core.Disposable disposable -> disposable.dispose();
                case Future<?> future -> future.cancel(true);
                default -> {
                    log.warn("task is not a Disposable or a Future");
                    continue;
                }
            }
            cancelCount++;
        }
        tasks.clear();
        log.info("取消了: {}条任务", cancelCount);
    }

    /// agent会话信息
    public String userId;
    @Getter
    public String agentId;
    public long connectTimestamp = 0L;
    public ConnectionSession session;
    @Getter
    public McpSwitch mcpSwitch = new McpSwitch();
    public MixLLMManager mixLLMManager = new MixLLMManager();

    /// vision chat
    // image chat
    public StringBuffer imageBase64 = new StringBuffer();
    // image list chat (开发的时候再放出来)
//    public List<StringBuffer> imageListBase64 = new ArrayList<>();
    // video chat (开发的时候再放出来)
//    public StringBuffer videoBase64 = new StringBuffer();
    public VisionContext visionContext = new VisionContext();

    /// userQuestion
    @Getter
    @Setter
    private String userRequestQuestion = "";
    public StringBuffer agentResponseStringBuffer = new StringBuffer();
    private final AtomicInteger currentAgentResponseCount = new AtomicInteger(0);

    /// 当前聊天会话信息
    private String currentMessageId = String.valueOf(IdUtil.getSnowflake().nextId());
    public long currentUserMessageTimestamp = System.currentTimeMillis();
    public long currentAgentMessageTimestamp = System.currentTimeMillis();
    public LocalDateTime currentMessageDateTime = LocalDateTime.now();

    // 开启新的一问一答
    public void newChatMessage(){
        // 重置
        reset();
        mcpSwitch.camera = McpSwitch.McpSwitchMode.FREELY.code;

        // 填充新的会话数据
        currentMessageId = String.valueOf(IdUtil.getSnowflake().nextId());
        currentUserMessageTimestamp = System.currentTimeMillis();
        currentMessageDateTime = LocalDateTime.now();

        log.info("开启新的message，MessageId是：{}", currentMessageId);
    }

    public String getCurrentContextParam(){
        Map<String, String> param = Map.of(
                "userId", userId,
                "agentId", agentId,
                "messageId", currentMessageId/*,
                "timestamp", currentMessageDateTime.toString(),
                "userQuestion", userQuestion*/
        );
        return JSON.toJSONString(param);
    }

    private void sendEOF(){
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.STOP_TTS.getType());
        responseMap.put(RealtimeResponseDataTypeEnum.DATA, RealtimeResponseDataTypeEnum.STOP_TTS.getType());
        String endResponse = JSON.toJSONString(responseMap);
        webSocketMessageManager.submitMessage(
                agentId,
                endResponse
        );

        // 重置
        reset();
    }

    // TTS 是否结束
    public boolean isTTSFinallyFinish(){
        // 只有当LLM结束并且TTS的MQ不存在句子时候才跳出TTS自我调用: !isLLMing && !haveSentence
        boolean isLLMing = llmProxyContext.isLLMing();
        boolean haveSentence = llmProxyContext.getAllTTSCount() > 0;
        // LLM结束 && TTS的MQ不存在句子
        return !isLLMing && !haveSentence;
    }

    /// ==========IRealTimeChatResponseManager==========

    @Override
    public @NonNull RealtimeChatTextResponse getUpToNowAgentResponse() {
        val response = new RealtimeChatTextResponse();
        response.setAgentId(agentId);
        response.setUserId(userId);
        response.setRole(RoleTypeEnum.AGENT.getValue());
        response.setContent(agentResponseStringBuffer.toString());
        response.setMessageId(getCurrentAgentMessageId());
        response.setTimestamp(currentAgentMessageTimestamp);
        response.setChatTime(getCurrentMessageTimeStr());
        return response;
    }

    @Override
    public @NonNull RealtimeChatTextResponse getCurrentFragmentAgentResponse(@NonNull String fragmentText) {
        val response = new RealtimeChatTextResponse();
        response.setAgentId(agentId);
        response.setUserId(userId);
        response.setRole(RoleTypeEnum.AGENT.getValue());
        response.setContent(fragmentText);
        response.setMessageId(getCurrentAgentMessageId());
        response.setTimestamp(currentAgentMessageTimestamp);
        response.setChatTime(getCurrentMessageTimeStr());
        return response;
    }

    @Override
    public @NonNull RealtimeChatTextResponse getUserSTTResultResponse(@NonNull String sttResult) {
        setUserRequestQuestion(sttResult);
        val response = new RealtimeChatTextResponse();
        response.setAgentId(agentId);
        response.setUserId(userId);
        response.setRole(RoleTypeEnum.USER.getValue());
        response.setContent(sttResult);
        response.setMessageId(getCurrentUserMessageId());
        response.setTimestamp(currentUserMessageTimestamp);
        response.setChatTime(getCurrentMessageTimeStr());
        return response;
    }

    @Override
    public @NotNull RealtimeChatTextResponse getUserTextResponse(@NonNull String userChatText){
        setUserRequestQuestion(userChatText);
        val response = new RealtimeChatTextResponse();
        response.setAgentId(agentId);
        response.setUserId(userId);
        response.setRole(RoleTypeEnum.USER.getValue());
        response.setContent(userChatText);
        response.setMessageId(getCurrentUserMessageId());
        response.setTimestamp(currentUserMessageTimestamp);
        response.setChatTime(getCurrentMessageTimeStr());
        return response;
    }

    private String getAgentMessageHeaderId(){
        return RoleTypeEnum.AGENT.getValue() + String.valueOf(currentAgentResponseCount.get());
    }
    public int addAgentMessageHeaderCount(){
        return currentAgentResponseCount.addAndGet(1);
    }
    private String getUserMessageHeaderId(){
        return RoleTypeEnum.USER.getValue() + "0";
    }
    @NonNull
    private String getCurrentAgentMessageId(){
        return getAgentMessageHeaderId() + ":" + currentMessageId;
    }
    @NonNull
    private String getCurrentUserMessageId(){
        return getUserMessageHeaderId() + ":" + currentMessageId;
    }
    @NonNull
    private String getCurrentMessageTimeStr(){
        return DateUtils.yyyyMMddHHmmssToString(this.currentMessageDateTime);
    }

    /// ==========RealtimeProcess==========

    @Override
    public void startRecord() {
        llmProxyContext.startRecord();
        log.info("[Audio] 录音开始");
    }

    @Override
    public void stopRecord() {
        llmProxyContext.stopRecord();
        log.info("[Audio] 录音结束");
    }

    @Override
    public void startLLM() {
        llmProxyContext.startLLM();
        log.info("[LLM] 开始LLM");
    }

    @Override
    public void stopLLM() {
        llmProxyContext.stopLLM();
        log.info("[LLM] 停止LLM");
    }

    @Override
    public void startTTS() {
        llmProxyContext.startTTS();
        log.info("[TTS] 开始TTS");
    }

    @Override
    public void stopTTS() {
        llmProxyContext.stopTTS();
        log.info("[TTS] 停止TTS");
    }

    @Override
    public void endConversation() {
        llmProxyContext.endConversation();
        sendEOF();
        log.info("[Session] 结束会话");
    }

    @Override
    public void reset() {
        llmProxyContext.reset();
        visionContext.reset();
        mixLLMManager.reset();
        // 取消正在执行的任务
        cancelTask(chatTasks);
        cancelTask(functionCallTasks);
        // 取消任务并不会清除userQuestion
        userRequestQuestion = "";
        agentResponseStringBuffer.setLength(0);
        currentAgentResponseCount.set(0);

        log.info("[Session] 重置会话");
    }

    ///==========ChatRealtimeState==========

    public int[] addCountAndCheckIsOverLimit(){
        int[] countAndLimit = new int[2];
        int currentLLMErrorCount = getLLMErrorCount();
        boolean isOverLimit = currentLLMErrorCount >= ModelConstant.LLM_CONNECT_RESET_MAX_RETRY_COUNT;
        countAndLimit[0] = currentLLMErrorCount;
        countAndLimit[1] = isOverLimit ? 1 : 0;
        return countAndLimit;
    }

    @Override
    public int getLLMErrorCount() {
        return llmProxyContext.getLLMErrorCount();
    }

    @Override
    public boolean isLLMing() {
        return llmProxyContext.isLLMing();
    }

    @Override
    public boolean isAudioChat() {
        return llmProxyContext.isAudioChat();
    }

    @Override
    public boolean isRecording() {
        return llmProxyContext.isRecording();
    }

    @Override
    public void setIsFirstTTS(boolean isFirstTTS) {
        llmProxyContext.setIsFirstTTS(isFirstTTS);
    }

    @Override
    public AtomicBoolean isFirstTTS() {
        return llmProxyContext.isFirstTTS();
    }

    @Override
    public boolean isTTSing() {
        return llmProxyContext.isTTSing();
    }

    @Override
    public long getTTSStartTime() {
        return llmProxyContext.getTTSStartTime();
    }

    @Override
    public void setTTSStartTime(long time) {
        llmProxyContext.setTTSStartTime(time);
    }


    @Override
    public void addFunctionCallResult(String result) {
        llmProxyContext.addFunctionCallResult(result);
    }

    @NotNull
    @Override
    public String getAllFunctionCallResult() {
        return llmProxyContext.getAllFunctionCallResult();
    }

    @Override
    public void setIsFinalResultTTS(boolean isFinalResultTTS) {
        llmProxyContext.setIsFinalResultTTS(isFinalResultTTS);
    }

    @Override
    public boolean isFinalResultTTS() {
        return llmProxyContext.isFinalResultTTS();
    }
}
