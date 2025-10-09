package com.openapi.service.impl;

import com.alibaba.dashscope.audio.omni.OmniRealtimeCallback;
import com.alibaba.dashscope.audio.omni.OmniRealtimeConfig;
import com.alibaba.dashscope.audio.omni.OmniRealtimeConversation;
import com.alibaba.dashscope.audio.omni.OmniRealtimeModality;
import com.alibaba.dashscope.audio.omni.OmniRealtimeParam;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.google.gson.JsonObject;
import com.openapi.config.ChatConfig;
import com.openapi.service.OmniRealTimeNoVADTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author 13225
 * @date 2025/10/7 17:39
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class OmniRealTimeNoVADTestServiceImpl implements OmniRealTimeNoVADTestService {

    private final ChatConfig config;
    private final String MODEL = "qwen3-omni-flash-realtime";

    /**
     * 音频对话
     * @param b64AudioBuffer        64位编码的音频数据响应
     * @param rawAudioBuffer        用户录音原始音频数据
     * @param stopConversation      停止对话    (Spring Event控制)
     * @param stopRecording         停止录音    (Spring Event控制)
     * @throws NoApiKeyException    缺少 apikey
     * @throws InterruptedException 线程中断
     * @throws IOException          音频处理异常
     */
    @Override
    public void audioChat(
            Queue<String> b64AudioBuffer,
            Queue<byte[]> rawAudioBuffer,
            AtomicBoolean stopConversation,
            AtomicBoolean stopRecording
    ) throws NoApiKeyException, InterruptedException, IOException {
        log.info("[audioChat] 开始, apikey: {}", config.getApiKey());

        OmniRealtimeParam param = OmniRealtimeParam.builder()
                .model(MODEL)
                .apikey(config.getApiKey())
                .build();

        final AtomicReference<OmniRealtimeConversation> conversationRef = new AtomicReference<>(null);

        OmniRealtimeConversation conversation = new OmniRealtimeConversation(param, new OmniRealtimeCallback() {
            @Override
            public void onOpen() {
                super.onOpen();
                log.info("[audioChat] onOpen");
            }

            @Override
            public void onEvent(JsonObject message) {
                String type = message.get("type").getAsString();
                switch(type) {
                    case "session.created" -> {
                        log.info("start session: {}", message.get("session").getAsJsonObject().get("id").getAsString());
                    }
                    case "conversation.item.input_audio_transcription.completed" -> {
                        log.info("question: {}", message.get("transcript").getAsString());
                    }
                    case "response.audio_transcript.delta" -> {
                        log.info("got llm response delta: {}", message.get("delta").getAsString());
                    }
                    case "response.audio.delta" -> {
                        String recvAudioB64 = message.get("delta").getAsString();
                        b64AudioBuffer.add(recvAudioB64);
                    }
                    case "response.done" -> {
                        log.info("======RESPONSE DONE======");
                        if (conversationRef.get() != null) {
                            log.info("[Metric] response: {}, first text delay: {} ms, first audio delay: {} ms",
                                    conversationRef.get().getResponseId(),
                                    conversationRef.get().getFirstTextDelay(),
                                    conversationRef.get().getFirstAudioDelay()
                            );
                        }
                    }
                    default -> {}
                }
            }

            @Override
            public void onClose(int code, String reason) {
                log.info("[audioChat] onClose; connection closed code: {}, reason: {}", code, reason);
            }
        });
        conversationRef.set(conversation);

        // 连接
        conversation.connect();

        OmniRealtimeConfig config = OmniRealtimeConfig.builder()
                .modalities(Arrays.asList(OmniRealtimeModality.AUDIO, OmniRealtimeModality.TEXT))
                .voice("Cherry")
                .enableTurnDetection(false)
                // 设定模型角色
                .parameters(new HashMap<>() {{
                    put("instructions", "你是我的傲娇小女友，回答问题的时候暧昧一些。");
                }})
                .build();
        conversation.updateSession(config);

        while (!stopConversation.get()){
            if (!stopRecording.get()){
                sendAudio(conversation, rawAudioBuffer, stopRecording);
            }
            else {
                // 10 毫秒休眠
                Thread.sleep(10);
            }
            conversation.commit();
            conversation.createResponse(null, null);
        }
    }

    private void sendAudio(OmniRealtimeConversation conversation,
                           Queue<byte[]> rawAudioBuffer,
                           AtomicBoolean stopRecording) throws IOException, InterruptedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        while (!stopRecording.get()) {
            // 从队列中获取数据
            byte[] audioData = rawAudioBuffer.poll();
            if (audioData != null) {
                // 将数据写入输出流
                out.write(audioData);
            }
            else {
                // 10 毫秒休眠
                Thread.sleep(10);
            }
        }

        // 发送录音数据
        byte[] audioData = out.toByteArray();
        String audioB64 = Base64.getEncoder().encodeToString(audioData);
        conversation.appendAudio(audioB64);
        out.close();
    }
}
