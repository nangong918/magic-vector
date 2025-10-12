package com.openapi.service.impl;

import com.alibaba.dashscope.audio.omni.OmniRealtimeCallback;
import com.alibaba.dashscope.audio.omni.OmniRealtimeConfig;
import com.alibaba.dashscope.audio.omni.OmniRealtimeConversation;
import com.alibaba.dashscope.audio.omni.OmniRealtimeModality;
import com.alibaba.dashscope.audio.omni.OmniRealtimeParam;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.fastjson.JSON;
import com.google.gson.JsonObject;
import com.openapi.config.ChatConfig;
import com.openapi.domain.constant.realtime.RealtimeDataTypeEnum;
import com.openapi.service.OmniRealTimeNoVADTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
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
     * 获取会话
     * @param session       websocket会话
     * @return              OmniRealtime会话
     */
    @Override
    public OmniRealtimeConversation getOmniRealtimeConversation(
            @NotNull WebSocketSession session){
        log.info("[获取会话] apikey: {}", config.getApiKey());

        OmniRealtimeParam param = OmniRealtimeParam.builder()
                .model(MODEL)
                .apikey(config.getApiKey())
                .build();
        final AtomicReference<OmniRealtimeConversation> conversationRef = new AtomicReference<>(null);

        return new OmniRealtimeConversation(param, new OmniRealtimeCallback() {
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
                        log.info("session.created");
                    }
                    case "conversation.item.input_audio_transcription.completed" -> {
                        log.info("question: {}", message.get("transcript").getAsString());
                    }
                    case "response.audio_transcript.delta" -> {
                        log.info("got llm response delta: {}", message.get("delta").getAsString());
                        Map<String, String> responseMap = new HashMap<>();
                        responseMap.put(RealtimeDataTypeEnum.TYPE, RealtimeDataTypeEnum.START.getType());
                        responseMap.put(RealtimeDataTypeEnum.DATA, RealtimeDataTypeEnum.START.getType());
                        String response = JSON.toJSONString(responseMap);
                        try {
                            session.sendMessage(new TextMessage(response));
                        } catch (IOException e) {
                            log.error("[websocket error] 响应消息异常", e);
                        }
                    }
                    case "response.audio.delta" -> {
                        String b64Audio = message.get("delta").getAsString();
//                        responseAudioBuffer.add(b64Audio);
                        Map<String, String> responseMap = new HashMap<>();
                        responseMap.put(RealtimeDataTypeEnum.TYPE, RealtimeDataTypeEnum.AUDIO_CHUNK.getType());
                        responseMap.put(RealtimeDataTypeEnum.DATA, b64Audio);
                        String response = JSON.toJSONString(responseMap);
                        try {
                            session.sendMessage(new TextMessage(response));
                        } catch (IOException e) {
                            log.error("[websocket error] 响应消息异常", e);
                        }
                    }
                    case "response.done" -> {
                        log.info("======RESPONSE DONE======");
                        Map<String, String> responseMap = new HashMap<>();
                        responseMap.put(RealtimeDataTypeEnum.TYPE, RealtimeDataTypeEnum.STOP.getType());
                        responseMap.put(RealtimeDataTypeEnum.DATA, RealtimeDataTypeEnum.STOP.getType());
                        String response = JSON.toJSONString(responseMap);
                        try {
                            session.sendMessage(new TextMessage(response));
                        } catch (IOException e) {
                            log.error("[websocket error] 响应消息异常", e);
                        }
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
    }

    /**
     * 设置会话参数 + 启动会话
     * @param conversation          会话
     * @throws NoApiKeyException    缺少 apikey
     * @throws InterruptedException 线程中断
     */
    @Override
    public void setOmniRealtimeConversationConfig(@NotNull OmniRealtimeConversation conversation) throws NoApiKeyException, InterruptedException {
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
    }

    /**
     * 启动会话
     * @param conversation              会话
     * @param stopRecording             停止录音
     * @param requestAudioBuffer        请求音频数据
     * @throws IOException              输入输出异常
     * @throws InterruptedException     线程中断
     */
    @Override
    public void startChat(@NotNull OmniRealtimeConversation conversation,
                          @NotNull AtomicBoolean stopRecording,
                          @NotNull Queue<byte[]> requestAudioBuffer) throws IOException, InterruptedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        log.info("[audioChat] 开始将音频流数据填充缓冲区");
        while (!stopRecording.get()) {
            // 从队列中获取数据
            byte[] audioData = requestAudioBuffer.poll();

            if (audioData != null) {
                // 将数据写入输出流
                int length = audioData.length;
                if (length > 0) {
                    log.info("[audioChat] 填充缓冲区数据长度: {}", length);
                    out.write(audioData, 0, length);
                }
            }
            else {
                // 10 毫秒休眠
                Thread.sleep(10);
            }
        }

        // 发送录音数据
        byte[] audioData = out.toByteArray();
        String audioB64 = Base64.getEncoder().encodeToString(audioData);
        log.info("[audioChat] 音频数据开始添加, 音频数据长度： {}", audioB64.length());
        conversation.appendAudio(audioB64);
        out.close();

        log.info("[audioChat] 会话提交");
        conversation.commit();
        conversation.createResponse(null, null);
    }

}
