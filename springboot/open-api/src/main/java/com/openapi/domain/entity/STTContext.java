package com.openapi.domain.entity;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 13225
 * @date 2025/11/7 11:56
 * 设计模式：组合模式：组合优于继承
 */
public class STTContext {
    // 是否是音频会话
    private final AtomicBoolean isAudioChat = new AtomicBoolean(false);
    // 是否正在录音
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    // user的音频数据（STT接收的是Flowable<BufferArray>, 必须提前解析好为byte[]）
    private final Queue<byte[]> requestAudioBuffer = new ConcurrentLinkedQueue<>();
    // 录音重试次数
    private final AtomicInteger recordRetryCount = new AtomicInteger(0);
    // 获取重试次数
    public int getRecordRetryCountAndIncrement() {
        return recordRetryCount.incrementAndGet();
    }
    /**
     * 录音开始
     */
    public void startRecord() {
        isRecording.set(true);
        isAudioChat.set(true);
    }

    /**
     * 添加音频数据
     * @param buffer    音频数据
     */
    public void offerAudioBuffer(byte[] buffer) {
        requestAudioBuffer.offer(buffer);
    }

    /**
     * 获取音频数据
     * @return  音频数据
     */
    public byte[] pollAudioBuffer() {
        return requestAudioBuffer.poll();
    }

    /**
     * 是否是音频会话
     * @return  true/false
     */
    public boolean isAudioChat() {
        return isAudioChat.get();
    }

    /**
     * 是否正在录音
     * @return  true/false
     */
    public boolean isRecording() {
        return isRecording.get();
    }

    /**
     * 录音结束
     */
    public void stopRecord() {
        isRecording.set(false);
    }

    /**
     * 重置
     */
    public void reset() {
        requestAudioBuffer.clear();
        isAudioChat.set(false);
        isRecording.set(false);
        recordRetryCount.set(0);
    }
}
