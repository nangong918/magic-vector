package com.openapi.domain.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author 13225
 * @date 2025/11/7 12:52
 */
public class TTSContext {
    // 是否是首次TTS
    private final AtomicBoolean isFirstTTS = new AtomicBoolean(true);
    // 上一次TTS的时间
    @Getter
    @Setter
    private long lastTTS = 0L;
    // 是否正在生成音频
    private final AtomicBoolean isTTSing = new AtomicBoolean(false);

    /**
     * 是否是首次TTS
     * @return  true/false
     */
    public AtomicBoolean isFirstTTS() {
        return isFirstTTS;
    }

    /**
     * 设置是否是首次TTS
     * @param firstTTS  true/false
     */
    public void setFirstTTS(boolean firstTTS) {
        isFirstTTS.set(firstTTS);
    }

    /**
     * 是否正在生成音频
     * @return  true/false
     */
    public boolean isTTSing() {
        return isTTSing.get();
    }

    /**
     * 设置是否正在生成音频
     * @param ttsing  true/false
     */
    public void setTTSing(boolean ttsing) {
        isTTSing.set(ttsing);
    }

    /**
     * 重置
     */
    public void reset() {
        isFirstTTS.set(true);
        isTTSing.set(false);
        lastTTS = 0L;
    }
}
