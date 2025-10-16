package com.openapi.component.manager;

import org.springframework.web.socket.WebSocketSession;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author 13225
 * @date 2025/10/16 9:50
 * 实时聊天的上下文管理器，一个连接一个，并非单例，不呢个使用@Component
 */
public class RealtimeChatContextManager {

    // 音频数据
    public final Queue<byte[]> requestAudioBuffer = new ConcurrentLinkedQueue<>();
    public final AtomicBoolean stopRecording = new AtomicBoolean(true);

    // agent会话信息
    public String userId;
    public String agentId;
    public WebSocketSession session;

    // llm -> tts
    public final Queue<String> sentenceQueue = new ConcurrentLinkedQueue<>();
    public final AtomicBoolean isTTSFinished = new AtomicBoolean(true);
    public final AtomicBoolean isLLMFinished = new AtomicBoolean(false);


    // 当前聊天会话信息
    public StringBuilder currentResponse = new StringBuilder();


}
