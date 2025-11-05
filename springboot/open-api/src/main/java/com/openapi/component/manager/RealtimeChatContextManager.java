package com.openapi.component.manager;

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
 */
@Slf4j
public class RealtimeChatContextManager {
    /// chatClient      (chatModel是单例，但是chatClient需要集成Agent的记忆，以及每个chatClient的设定不同，所以不是单例)
    public ChatClient chatClient;

    /// 会话任务
    private volatile Future<?> chatFuture;
    private final List<Object> chatDisposables = new ArrayList<>();
    private volatile Future<?> visionChatFuture;
    private final List<Object> visionChatDisposables = new ArrayList<>();
    private volatile Future<?> ttsFuture;
    // todo 多线程取消还存在很多问题，思考清除全面测试
    public void setChatFuture(Future<?> chatFuture){
        cancelChatFuture();
        this.chatFuture = chatFuture;
    }
    public void setVisionChatFuture(Future<?> visionChatFuture){
        cancelVisionChatFuture();
        this.visionChatFuture = visionChatFuture;
    }
    public void addChatDisposables(Object chatDisposable){
        if (chatDisposable instanceof io.reactivex.disposables.Disposable || chatDisposable instanceof reactor.core.Disposable){
            chatDisposables.add(chatDisposable);
        }
    }
    public void cancelChatDisposable(){
        if (chatDisposables.isEmpty()){
            return;
        }
        else {
            log.info("取消之前的chatDisposable任务数量: {}", chatDisposables.size());
        }
        for (Object disposable : chatDisposables){
            if (disposable instanceof io.reactivex.disposables.Disposable){
                ((io.reactivex.disposables.Disposable) disposable).dispose();
            }
            else if (disposable instanceof reactor.core.Disposable){
                ((reactor.core.Disposable) disposable).dispose();
            }
        }
        chatDisposables.clear();
    }
    public void addVisionChatDisposables(Object visionChatDisposable){
        if (visionChatDisposable instanceof io.reactivex.disposables.Disposable || visionChatDisposable instanceof reactor.core.Disposable){
            visionChatDisposables.add(visionChatDisposable);
        }
    }
    public void cancelVisionChatDisposable(){
        if (visionChatDisposables.isEmpty()){
            return;
        }
        else {
            log.info("取消之前的visionChatDisposable任务数量: {}", visionChatDisposables.size());
        }
        for (Object disposable : visionChatDisposables){
            if (disposable instanceof io.reactivex.disposables.Disposable){
                ((io.reactivex.disposables.Disposable) disposable).dispose();
            }
            else if (disposable instanceof reactor.core.Disposable){
                ((reactor.core.Disposable) disposable).dispose();
            }
        }
        visionChatDisposables.clear();
    }
    public void setTtsFuture(Future<?> ttsFuture){
        cancelTtsFuture();
        this.ttsFuture = ttsFuture;
    }
    public void cancelChatFuture(){
        if (this.chatFuture != null){
            log.info("取消之前的chat任务");
            this.chatFuture.cancel(true);
        }
        cancelChatDisposable();

        // 数据复位
        currentResponseStringBuffer.setLength(0);
        sentenceQueue.clear();
        isLLMFinished.set(false);
        llmConnectResetRetryCount.set(0);
        currentAgentResponseCount.set(0);
    }
    public void cancelVisionChatFuture(){
        if (this.visionChatFuture != null){
            log.info("取消之前的visionChat任务");
            this.visionChatFuture.cancel(true);
        }
        cancelVisionChatDisposable();

        requestAudioBuffer.clear();
        stopRecording.set(true);
        isTTSFinished.set(true);
        isFirstTTS.set(true);
        lastTTSTimestamp = 0L;
        isVisionChat.set(false);
        isVisionChatFinished.set(false);
    }
    public void cancelTtsFuture(){
        if (this.ttsFuture != null){
            log.info("取消之前的tts任务");
            this.ttsFuture.cancel(true);
        }
        imageBase64.setLength(0);
    }

    /// 音频数据
    public final Queue<byte[]> requestAudioBuffer = new ConcurrentLinkedQueue<>();
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
    // 是否是视觉任务 （解决：1.视觉任务的第一次agent call不能发送eof 2.视觉任务的启动不能发送起始符号）
    public final AtomicBoolean isVisionChat = new AtomicBoolean(false);
    // 视觉任务是否结束
    public final AtomicBoolean isVisionChatFinished = new AtomicBoolean(false);

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

    // 取消当前的任务
    public void cancelCurrentTask(){
        cancelChatFuture();
        cancelVisionChatFuture();
        cancelTtsFuture();
        cancelChatDisposable();
        cancelVisionChatDisposable();

        chatFuture = null;
        visionChatFuture = null;
        ttsFuture = null;
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
    public String getCurrentAgentMessageId(){
        return getAgentMessageHeaderId() + ":" + currentMessageId;
    }

    @NonNull
    public String getCurrentUserMessageId(){
        return getUserMessageHeaderId() + ":" + currentMessageId;
    }

    @NonNull
    public String getCurrentMessageTimeStr(){
        return DateUtils.yyyyMMddHHmmssToString(this.currentMessageDateTime);
    }

    @NonNull
    public RealtimeChatTextResponse getCurrentResponse(){
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

    @NonNull
    public RealtimeChatTextResponse getCurrentFragmentResponse(@NonNull String fragmentText){
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

    @NonNull
    public RealtimeChatTextResponse getSTTResultResponse(@NonNull String sstResult){
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

    @NonNull
    public RealtimeChatTextResponse getUserTextResponse(@NonNull String userChatText){
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
}
