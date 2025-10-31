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
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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

    // 音频数据
    public final Queue<byte[]> requestAudioBuffer = new ConcurrentLinkedQueue<>();
    public final AtomicBoolean stopRecording = new AtomicBoolean(true);

    // agent会话信息
    public String userId;
    public String agentId;
    public long connectTimestamp = 0L;
    public WebSocketSession session;

    // llm -> tts
    public final Queue<String> sentenceQueue = new ConcurrentLinkedQueue<>();
    public final AtomicBoolean isTTSFinished = new AtomicBoolean(true);
    public final AtomicBoolean isLLMFinished = new AtomicBoolean(false);
    // 用于记录是否是首次TTS，因为后续都是2+句话一个语音生成；而首次是单个句子
    public final AtomicBoolean isFirstTTS = new AtomicBoolean(true);
    // 实现语音延迟
    public long lastTTSTimestamp = 0L;
    // llm Connect Reset重试
    public final AtomicInteger llmConnectResetRetryCount = new AtomicInteger(0);

    // userQuestion
    @Getter
    @Setter
    private String userQuestion = "";

    // 当前聊天会话信息
    private String currentMessageId = String.valueOf(IdUtil.getSnowflake().nextId());
    public long currentUserMessageTimestamp = System.currentTimeMillis();
    public long currentAgentMessageTimestamp = System.currentTimeMillis();
    public LocalDateTime currentMessageDateTime = LocalDateTime.now();
    public StringBuffer currentResponseStringBuffer = new StringBuffer();
    // 开启新的一问一答 todo 需要停止之前全部的模型消息和缓存
    public void newChatMessage(){
        userQuestion = "";
        currentMessageId = String.valueOf(IdUtil.getSnowflake().nextId());
        currentUserMessageTimestamp = System.currentTimeMillis();
        currentMessageDateTime = LocalDateTime.now();
        currentResponseStringBuffer = new StringBuffer();
        isFirstTTS.set(true);
        llmConnectResetRetryCount.set(0);
        lastTTSTimestamp = 0L;
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

    @NonNull
    public String getCurrentAgentMessageId(){
        return RoleTypeEnum.AGENT.getValue() + ":" + currentMessageId;
    }

    @NonNull
    public String getCurrentUserMessageId(){
        return RoleTypeEnum.USER.getValue() + ":" + currentMessageId;
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
    public RealtimeChatTextResponse getSSTResultResponse(@NonNull String sstResult){
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

    public void clear() {
    }
}
