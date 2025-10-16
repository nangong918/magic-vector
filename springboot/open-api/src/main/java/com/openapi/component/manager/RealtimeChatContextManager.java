package com.openapi.component.manager;

import cn.hutool.core.util.IdUtil;
import com.openapi.domain.constant.RoleTypeEnum;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author 13225
 * @date 2025/10/16 9:50
 * 实时聊天的上下文管理器，一个连接一个，并非单例，不能使用@Component
 * 需求：1. 用户发送语音 + 回调显示用户语音 2. 流式音频回调 3. 流式文本持续回显 4. 聊天记录保存到mysql 5.下次会话的时候加载到ChatModel
 */
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


    // 当前聊天会话信息
    private String currentMessageId = String.valueOf(IdUtil.getSnowflake().nextId());
    public long currentMessageTimestamp = System.currentTimeMillis();
    public LocalDateTime currentMessageDateTime = LocalDateTime.now();
    public StringBuilder currentResponse = new StringBuilder();
    // 开启新的一问一答 todo 需要停止之前全部的模型消息和缓存
    public void newChatMessage(){
        currentMessageId = String.valueOf(IdUtil.getSnowflake().nextId());
        currentMessageTimestamp = System.currentTimeMillis();
        currentMessageDateTime = LocalDateTime.now();
        currentResponse = new StringBuilder();
    }
    public String getCurrentAgentMessageId(){
        return RoleTypeEnum.AGENT.getValue() + ":" + currentMessageId;
    }
    public String getCurrentUserMessageId(){
        return RoleTypeEnum.USER.getValue() + ":" + currentMessageId;
    }

    public void clear() {
    }
}
