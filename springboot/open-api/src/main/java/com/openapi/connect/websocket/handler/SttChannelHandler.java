package com.openapi.connect.websocket.handler;

import com.google.gson.Gson;
import com.openapi.connect.websocket.manager.WsMessageSenderManager;
import com.openapi.connect.websocket.service.WsConversationService;
import com.openapi.domain.constant.ws.WsChannel;
import com.openapi.domain.constant.ws.WsEvent;
import com.openapi.domain.dto.ws.base.ClientEvent;
import com.openapi.domain.dto.ws.base.ServerEvent;
import com.openapi.domain.dto.ws.request.SttAudioDataRequest;
import com.openapi.domain.dto.ws.request.SttEndRequest;
import com.openapi.domain.dto.ws.request.SttStartRequest;
import com.openapi.domain.dto.ws.response.SttErrorResponse;
import com.openapi.domain.dto.ws.response.SttStartResponse;
import com.openapi.domain.dto.ws.response.SttTextDataResponse;
import com.openapi.interfaces.mixLLM.STTCallback;
import com.openapi.interfaces.model.StreamCallErrorCallback;
import com.openapi.service.model.STTServiceService;
import io.reactivex.disposables.Disposable;
import io.reactivex.processors.PublishProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public class SttChannelHandler implements ChannelHandler {

    /** STT 模型服务：消费音频流并回调识别结果。 */
    private final STTServiceService sttServiceService;
    /** 语音会话编排器：STT 完成后进入 LLM / instruction_list 链路。 */
    private final WsConversationService wsConversationService;
    /** 统一 WS 下行发送器（带出站队列）。 */
    private final WsMessageSenderManager wsMessageSender;
    /** DTO <-> Map 序列化工具。 */
    private final Gson gson;

    // 一个会话可并发多个 agent 语音流程，key=sessionId:agentId。
    private final ConcurrentHashMap<String, SttContext> sttContexts = new ConcurrentHashMap<>();

    @Override
    public void handle(WebSocketSession session, ClientEvent event) {
        // STT 通道只处理 stt_* 事件，其余事件直接忽略。
        WsEvent wsEvent = WsEvent.fromValue(event.getEvent());
        switch (wsEvent) {
            case STT_START -> handleSttStart(session, event);
            case STT_AUDIO_DATA -> handleSttAudioData(session, event);
            case STT_END -> handleSttEnd(session, event);
            case STT_ERROR -> handleSttError(session, event);
            default -> log.debug("Ignore unsupported stt event: {}", event.getEvent());
        }
    }

    /**
     * 处理 stt_start：
     * 1) 创建/替换当前 session-agent 的 STT 上下文；
     * 2) 启动 STT 流服务并绑定回调；
     * 3) 返回 stt_start_ack。
     */
    private void handleSttStart(WebSocketSession session, ClientEvent event) {
        SttStartRequest request = gson.fromJson(gson.toJson(event.getData()), SttStartRequest.class);
        if (request == null || request.getAgentId() == null || request.getAgentId().isBlank()) {
            return;
        }
        String key = buildKey(session.getId(), request.getAgentId());
        // 重复 start 时先清理旧上下文，避免同一 agent 出现双流并发。
        SttContext old = sttContexts.remove(key);
        if (old != null) {
            old.processor.onComplete();
            if (old.disposable != null && !old.disposable.isDisposed()) {
                old.disposable.dispose();
            }
        }
        SttContext context = new SttContext(request.getAgentId());
        sttContexts.put(key, context);

        // STT 回调只做两件事：实时回推文本 + 完成后交给会话编排器。
        STTCallback callback = new STTCallback() {
            @Override
            public void onSTTStart() {
                SttStartResponse response = new SttStartResponse();
                response.setAgentId(context.agentId);
                response.setCode(200);
                response.setMessage("OK");
                wsMessageSender.send(session, new ServerEvent(
                        WsChannel.STT.getValue(),
                        WsEvent.STT_START_ACK.getValue(),
                        Map.of("payload", gson.toJson(response))
                ));
            }

            @Override
            public void onIdentifying(String intermediateResult) {
                pushSttText(session, context, intermediateResult);
            }

            @Override
            public void onRecognitionSentence(String sentence) {
                if (sentence != null && !sentence.isBlank()) {
                    context.finalText.append(sentence);
                }
                pushSttText(session, context, sentence);
            }

            @Override
            public void onRecognitionComplete() {
                wsConversationService.handleSttFinal(session, context.agentId, context.finalText.toString());
                cleanup(session.getId(), context.agentId);
            }

            @Override
            public void onRecognitionError(Throwable e) {
                log.error("STT recognition error, sessionId={}", session.getId(), e);
            }

            @Override
            public void onSTTError(Throwable e) {
                SttErrorResponse response = new SttErrorResponse();
                response.setAgentId(context.agentId);
                response.setCode(500);
                response.setMessage(e.getMessage() == null ? "stt error" : e.getMessage());
                wsMessageSender.send(session, new ServerEvent(
                        WsChannel.STT.getValue(),
                        WsEvent.STT_ERROR.getValue(),
                        Map.of("payload", gson.toJson(response))
                ));
                cleanup(session.getId(), context.agentId);
            }

            @Override
            public void recordDisposable(Disposable disposable) {
                context.disposable = disposable;
            }
        };

        StreamCallErrorCallback errorCallback = new StreamCallErrorCallback() {
            @Override
            public int[] addCountAndCheckIsOverLimit() {
                int current = context.retry.incrementAndGet();
                return new int[]{current >= 3 ? 1 : 0, current};
            }

            @Override
            public void addTask(Object task) {
                if (task instanceof Disposable disposable) {
                    context.disposable = disposable;
                }
            }

            @Override
            public void endConversation() {
                cleanup(session.getId(), context.agentId);
            }
        };

        sttServiceService.sttStreamCallErrorProxy(
                context.processor.onBackpressureBuffer(),
                callback,
                errorCallback
        );
    }

    /**
     * 处理 stt_audio_data：
     * 1) 从 base64 解码字节；
     * 2) 推入对应上下文的音频流处理器。
     */
    private void handleSttAudioData(WebSocketSession session, ClientEvent event) {
        SttAudioDataRequest request = gson.fromJson(gson.toJson(event.getData()), SttAudioDataRequest.class);
        if (request == null || request.getAgentId() == null || request.getAgentId().isBlank()) {
            return;
        }
        SttContext context = sttContexts.get(buildKey(session.getId(), request.getAgentId()));
        if (context == null) {
            return;
        }
        if (request.getBase64AudioStream() == null || request.getBase64AudioStream().isBlank()) {
            return;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(request.getBase64AudioStream());
            context.processor.onNext(ByteBuffer.wrap(bytes));
        } catch (Exception e) {
            log.warn("Decode stt audio chunk failed, sessionId={}", session.getId(), e);
        }
    }

    /**
     * 处理 stt_end：关闭当前音频输入流，触发 STT 收尾回调。
     */
    private void handleSttEnd(WebSocketSession session, ClientEvent event) {
        SttEndRequest request = gson.fromJson(gson.toJson(event.getData()), SttEndRequest.class);
        if (request == null || request.getAgentId() == null || request.getAgentId().isBlank()) {
            return;
        }
        SttContext context = sttContexts.get(buildKey(session.getId(), request.getAgentId()));
        if (context == null) {
            return;
        }
        // 明确结束输入流，触发 STT onRecognitionComplete 回调。
        context.processor.onComplete();
    }

    /** 客户端主动上报 stt_error 的兜底日志入口。 */
    private void handleSttError(WebSocketSession session, ClientEvent event) {
        log.warn("Receive stt_error from client, sessionId={}, payload={}", session.getId(), event.getData());
    }

    /** 推送 STT 实时文本碎片（stt_text_data）。 */
    private void pushSttText(WebSocketSession session, SttContext context, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        SttTextDataResponse response = new SttTextDataResponse();
        response.setAgentId(context.agentId);
        response.setTextStream(text);
        response.setSeq(String.valueOf(context.seq.getAndIncrement()));
        response.setIsLast(false);
        response.setTimestamp(System.currentTimeMillis());
        wsMessageSender.send(session, new ServerEvent(
                WsChannel.STT.getValue(),
                WsEvent.STT_TEXT_DATA.getValue(),
                Map.of("payload", gson.toJson(response))
        ));
    }

    /** 回收并关闭一个 STT 上下文，避免会话泄露。 */
    private void cleanup(String sessionId, String agentId) {
        SttContext removed = sttContexts.remove(buildKey(sessionId, agentId));
        if (removed != null && removed.disposable != null && !removed.disposable.isDisposed()) {
            removed.disposable.dispose();
        }
    }

    private String buildKey(String sessionId, String agentId) {
        return sessionId + ":" + agentId;
    }

    /**
     * 单个 session-agent 的 STT 临时态：
     * - processor：音频输入流
     * - finalText：最终文本累加
     * - seq：实时文本分片序号
     * - retry：重试计数
     * - disposable：当前 STT 任务引用
     */
    private static class SttContext {
        private final String agentId;
        private final PublishProcessor<ByteBuffer> processor;
        private final StringBuilder finalText;
        private final AtomicInteger seq;
        private final AtomicInteger retry;
        private Disposable disposable;

        private SttContext(String agentId) {
            this.agentId = agentId;
            this.processor = PublishProcessor.create();
            this.finalText = new StringBuilder();
            this.seq = new AtomicInteger(0);
            this.retry = new AtomicInteger(0);
        }
    }
}
