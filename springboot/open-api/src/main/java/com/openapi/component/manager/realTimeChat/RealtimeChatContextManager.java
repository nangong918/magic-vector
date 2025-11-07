package com.openapi.component.manager.realTimeChat;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.openapi.domain.constant.RoleTypeEnum;
import com.openapi.domain.dto.ws.response.RealtimeChatTextResponse;
import com.openapi.utils.DateUtils;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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
public class RealtimeChatContextManager implements IRealTimeChatResponseManager {
    /// chatClient      (chatModel是单例，但是chatClient需要集成Agent的记忆，以及每个chatClient的设定不同，所以不是单例)
    public ChatClient chatClient;

    /// 会话任务
    private final List<Object> chatTasks = new ArrayList<>();
    private final List<Object> functionCallTasks = new ArrayList<>();

    /// 会话状态
    // 本次会话是否属于FunctionCall会话
    public AtomicBoolean isFunctionCall = new AtomicBoolean(false);
    // 是否正在会话
    public AtomicBoolean isChatting = new AtomicBoolean(false);

    /*
        会话状态：
        1. 未会话 -> agent回复中(包含llm和tts) -> 会话结束
        2. 未会话 -> 录音中 -> agent回复中(包含llm和tts) -> 会话结束
        3. 未会话 -> agent回复中 -> 等待function call结果 -> agent回复中 -> 会话结束
        4. 未会话 -> 录音中 -> agent回复中 -> 等待function call结果 -> agent回复中 -> 会话结束
        状态管理需要用AtomicResource, 并且使用synchronized上锁，使用并发设计模式
     */

    // 剩余等待的function call信号量 （封装Function Call）
    public final AtomicInteger remainingFunctionCallSignal = new AtomicInteger(0);

    // 解耦：非FunctionCall是一个完整流程，FunctionCall是另一个完整流程，互不干扰

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
        resetChatData();
    }
    public final void cancelFunctionCallTask(){
        cancelTask(functionCallTasks);
        resetFunctionCallData();
    }
    private void cancelTask(@NotNull List<Object> tasks){
        if (tasks.isEmpty()){
            return;
        }
        for (Object task: tasks){
            switch (task) {
                case null -> log.warn("task is null");
                case io.reactivex.disposables.Disposable disposable -> disposable.dispose();
                case reactor.core.Disposable disposable -> disposable.dispose();
                case Future<?> future -> future.cancel(true);
                default -> log.warn("task is not a Disposable or a Future");
            }
        }
        tasks.clear();
    }

    // 重置数据
    // 重置chat数据
    private void resetChatData(){
        // 取消chat之后，录音一定重置，function call不会调用此数据
        requestAudioBuffer.clear();
        stopRecording.set(true);
        // userQuestion不能取消，因为function call还会用到
//        userQuestion = "";
        // isChatting不能取消，因为还有function call，此处不能判定
//        isChatting.set(false);
        // 取消chat任务的时候需要将响应数据清空，避免跟function call的填充数据冲突。
        currentResponseStringBuffer.setLength(0);
        // 重置llm重连次数
        llmConnectResetRetryCount.set(0);
        // sentenceQueue不能取消，因为function call是继续填充，填充之后进行tts
//        sentenceQueue.clear();
        // llm需要设置为已经完成，因为function call会重新启动
        isLLMFinished.set(true);
        isTTSFinished.set(true);
        isFirstTTS.set(true);
        lastTTSTimestamp = 0L;
    }
    private void resetFunctionCallData(){
        // function call任务取消
        isFunctionCall.set(false);
        isChatting.set(false);
        // 响应数据清空
        userQuestion = "";
        currentResponseStringBuffer.setLength(0);
        currentAgentResponseCount.set(0);
        // 重置llm和tts数据
        llmConnectResetRetryCount.set(0);
        sentenceQueue.clear();
        isLLMFinished.set(true);
        isTTSFinished.set(true);
        isFirstTTS.set(true);
        lastTTSTimestamp = 0L;
        // vision
        imageBase64.setLength(0);
    }

    // 取消当前的任务
    public final void cancelCurrentTask(){
        cancelChatTask();
        cancelFunctionCallTask();
    }

    /// 音频数据
    // user音频数据
    public final Queue<byte[]> requestAudioBuffer = new ConcurrentLinkedQueue<>();
    // user是否停止录制
    public final AtomicBoolean stopRecording = new AtomicBoolean(true);

    /// agent会话信息
    public String userId;
    public String agentId;
    public long connectTimestamp = 0L;
    public WebSocketSession session;

    /// llm -> tts
    // llm
    // llm Connect Reset重试
    public final AtomicInteger llmConnectResetRetryCount = new AtomicInteger(0);
    public final Queue<String> sentenceQueue = new ConcurrentLinkedQueue<>();
    public final AtomicBoolean isLLMFinished = new AtomicBoolean(false);
    // tts
    public final AtomicBoolean isTTSFinished = new AtomicBoolean(true);
    // 用于记录是否是首次TTS，因为后续都是2+句话一个语音生成；而首次是单个句子
    public final AtomicBoolean isFirstTTS = new AtomicBoolean(true);
    // 实现语音延迟
    public long lastTTSTimestamp = 0L;

    /// vision chat
    // image chat
    public StringBuffer imageBase64 = new StringBuffer();
    // image list chat (开发的时候再放出来)
//    public List<StringBuffer> imageListBase64 = new ArrayList<>();
    // video chat (开发的时候再放出来)
//    public StringBuffer videoBase64 = new StringBuffer();


    /// userQuestion
    @Getter
    @Setter
    private String userQuestion = "";
    public StringBuffer currentResponseStringBuffer = new StringBuffer();
    private final AtomicInteger currentAgentResponseCount = new AtomicInteger(0);

    /// 当前聊天会话信息
    private String currentMessageId = String.valueOf(IdUtil.getSnowflake().nextId());
    public long currentUserMessageTimestamp = System.currentTimeMillis();
    public long currentAgentMessageTimestamp = System.currentTimeMillis();
    public LocalDateTime currentMessageDateTime = LocalDateTime.now();

    // 开启新的一问一答
    public void newChatMessage(){
        // 取消正在执行的任务
        cancelCurrentTask();
        // 取消任务并不会清除userQuestion
        userQuestion = "";

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


    /// ==========IRealTimeChatResponseManager==========

    @Override
    public @NonNull RealtimeChatTextResponse getUpToNowAgentResponse() {
        val response = new RealtimeChatTextResponse();
        response.setAgentId(agentId);
        response.setUserId(userId);
        response.setRole(RoleTypeEnum.AGENT.getValue());
        response.setContent(currentResponseStringBuffer.toString());
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
    public @NonNull RealtimeChatTextResponse getUserSTTResultResponse(@NonNull String sstResult) {
        setUserQuestion(sstResult);
        val response = new RealtimeChatTextResponse();
        response.setAgentId(agentId);
        response.setUserId(userId);
        response.setRole(RoleTypeEnum.USER.getValue());
        response.setContent(sstResult);
        response.setMessageId(getCurrentUserMessageId());
        response.setTimestamp(currentUserMessageTimestamp);
        response.setChatTime(getCurrentMessageTimeStr());
        return response;
    }

    @Override
    public @NotNull RealtimeChatTextResponse getUserTextResponse(@NonNull String userChatText){
        setUserQuestion(userChatText);
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

}
