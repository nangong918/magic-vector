package com.openapi.domain.constant;

import com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters;

/**
 * @author 13225
 * @date 2025/10/16 10:12
 */
public interface ModelConstant {

    String STT_Model = "fun-asr-realtime";
    String TTS_Model = "qwen3-tts-flash";
    String Chat_Model = "qwen-flash";
    long SENTENCE_INTERVAL = 300;
    AudioParameters.Voice TTS_Voice = AudioParameters.Voice.CHERRY;
    String TTS_LanguageType = "Chinese";

    String STT_Format = "pcm";
    int SST_SampleRate = 16000;
    String SYSTEM_PROMPT = "你是我的傲娇小女友，回答问题的时候暧昧一些。回答的之后只能输出正常语句, 不能使用表情等。对话精简一些，最好在3至5句话。";
}
