package com.openapi.connect.websocket.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.openapi.connect.websocket.manager.ConnectionManager;
import com.openapi.connect.websocket.manager.WsInstructionTrackerManager;
import com.openapi.connect.websocket.manager.WsMessageSenderManager;
import com.openapi.domain.ao.mixLLM.McpSwitch;
import com.openapi.domain.ao.mixLLM.MixLLMEvent;
import com.openapi.domain.ao.mixLLM.MixLLMResult;
import com.openapi.domain.constant.ws.Instruction;
import com.openapi.domain.constant.ws.WsChannel;
import com.openapi.domain.constant.ws.WsEvent;
import com.openapi.domain.dto.ws.base.ServerEvent;
import com.openapi.domain.dto.ws.request.InstructionResultRequest;
import com.openapi.domain.dto.ws.response.InstructionListResponse;
import com.openapi.domain.dto.ws.response.InstructionMcpItem;
import com.openapi.domain.dto.ws.response.InstructionTtsItem;
import com.openapi.domain.dto.ws.response.LlmDataResponse;
import com.openapi.domain.dto.ws.response.LlmEndResponse;
import com.openapi.domain.dto.ws.response.LlmStartResponse;
import com.openapi.domain.dto.ws.response.SystemMessageResponse;
import com.openapi.interfaces.model.TTSStateCallback;
import com.openapi.service.ChatMessageService;
import com.openapi.service.model.LLMServiceService;
import com.openapi.service.model.TTSServiceService;
import io.reactivex.disposables.Disposable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class WsConversationService {

    /** 连接会话管理，用于 session -> userId 映射。 */
    private final ConnectionManager connectionManager;
    /** instruction_list 回执计数器。 */
    private final WsInstructionTrackerManager trackerManager;
    /** 统一 WS 发送入口。 */
    private final WsMessageSenderManager wsMessageSender;
    /** 聊天记录持久化服务（user/agent 结果落库）。 */
    private final ChatMessageService chatMessageService;
    /** LLM 服务门面。 */
    private final LLMServiceService llmServiceService;
    /** TTS 服务门面。 */
    private final TTSServiceService ttsServiceService;
    /** Spring AI ChatClient。 */
    private final ChatClient chatClient;
    /** JSON 序列化工具。 */
    private final Gson gson;

    /**
     * 语音主链路编排：
     * STT最终文本 -> 落库(user) -> LLM -> 提取chatText并落库(agent) -> instruction_list 下发。
     */
    public void handleSttFinal(WebSocketSession session, String agentId, String sttFinalText) {
        if (agentId == null || agentId.isBlank()) {
            return;
        }
        String userId = connectionManager.getAndroidUserIdBySession(session);
        if (userId == null || userId.isBlank()) {
            log.warn("No userId bound for session {}, skip stt final handling", session.getId());
            return;
        }

        Long agentIdLong = parseLong(agentId);
        Long userIdLong = parseLong(userId);
        if (agentIdLong == null || userIdLong == null) {
            log.warn("Parse id failed, agentId={}, userId={}", agentId, userId);
            return;
        }

        String userText = sttFinalText == null ? "" : sttFinalText.trim();
        if (!userText.isEmpty()) {
            chatMessageService.insertOne(agentIdLong, userText, true, userIdLong);
        }

        sendLlmStart(session, agentId);

        String llmRaw = callLlm(agentId, userId, userText);
        String chatText = extractChatText(llmRaw);

        if (!chatText.isEmpty()) {
            chatMessageService.insertOne(agentIdLong, chatText, false, userIdLong);
        }

        sendLlmData(session, agentId, chatText);
        sendLlmEnd(session, agentId);

        List<Object> instructionItems = buildInstructions(agentId, llmRaw, chatText);

        if (instructionItems.isEmpty()) {
            sendAgentEndReply(session);
            return;
        }

        sendAgentStartReply(session);

        String requestId = UUID.randomUUID().toString();
        InstructionListResponse response = new InstructionListResponse();
        response.setRequestId(requestId);
        response.setAgentId(agentId);
        response.setInstructions(gson.toJson(instructionItems));

        trackerManager.register(requestId, session, userId, instructionItems.size());
        wsMessageSender.send(session, new ServerEvent(
                WsChannel.INSTRUCTION_LIST.getValue(),
                WsEvent.INSTRUCTION_LIST.getValue(),
                Map.of("payload", gson.toJson(response))
        ));
    }

    public void handleInstructionResult(WebSocketSession session, InstructionResultRequest request) {
        if (request == null || request.getRequestId() == null || request.getRequestId().isBlank()) {
            return;
        }
        WsInstructionTrackerManager.Tracker tracker = trackerManager.get(request.getRequestId());
        if (tracker == null) {
            return;
        }
        // 指令回执计数归并，全部完成后再发 agent_end_reply。
        boolean allDone = trackerManager.markDone(request.getRequestId());
        if (allDone) {
            // 当前 requestId 下所有指令都完成，通知客户端恢复空闲态。
            sendAgentEndReply(tracker.getSession());
            trackerManager.remove(request.getRequestId());
        }
    }

    /** 调用 LLM，返回原始字符串结果（可能为 JSONList，也可能为纯文本）。 */
    private String callLlm(String agentId, String userId, String sttText) {
        try {
            String contextParam = "userId:" + userId + ",agentId:" + agentId;
            String llmResult = llmServiceService.mixLLMCall(
                    sttText,
                    chatClient,
                    agentId,
                    contextParam,
                    new McpSwitch()
            );
            return llmResult == null ? "" : llmResult;
        } catch (Exception e) {
            log.error("LLM call failed, agentId={}", agentId, e);
            return "";
        }
    }

    /**
     * 从 LLM 原始结果中抽取可展示/可落库的 chatText：
     * - 优先按 MixLLMResult[] 解析并拼接 chatSentence；
     * - 解析失败时回退为原始文本。
     */
    private String extractChatText(String llmRaw) {
        if (llmRaw == null || llmRaw.isBlank()) {
            return "";
        }
        try {
            List<MixLLMResult> resultList = gson.fromJson(llmRaw, new TypeToken<List<MixLLMResult>>() {}.getType());
            if (resultList == null || resultList.isEmpty()) {
                return llmRaw;
            }
            StringBuilder sb = new StringBuilder();
            for (MixLLMResult item : resultList) {
                if (item != null && item.chatSentence != null && !item.chatSentence.isBlank()) {
                    if (!sb.isEmpty()) {
                        sb.append('\n');
                    }
                    sb.append(item.chatSentence.trim());
                }
            }
            return sb.toString().isBlank() ? llmRaw : sb.toString();
        } catch (Exception ignore) {
            return llmRaw;
        }
    }

    /**
     * 构建 instruction_list 混合编排：
     * - 优先按 MixLLMResult 顺序生成 [TTS, MCP, TTS, ...]；
     * - 解析失败时回退为纯 TTS 列表。
     */
    private List<Object> buildInstructions(String agentId, String llmRaw, String chatText) {
        List<MixLLMResult> parsed = parseMixResults(llmRaw);
        AtomicInteger indexGen = new AtomicInteger(0);
        List<Object> result = new ArrayList<>();

        if (!parsed.isEmpty()) {
            for (MixLLMResult item : parsed) {
                if (item == null) {
                    continue;
                }
                appendTtsItems(result, indexGen, agentId, item.chatSentence);
                appendMcpItems(result, indexGen, item.eventList);
            }
        } else {
            appendTtsItems(result, indexGen, agentId, chatText);
        }

        if (!result.isEmpty() && result.get(result.size() - 1) instanceof InstructionTtsItem last) {
            last.setIsLast(true);
        }
        return result;
    }

    private List<MixLLMResult> parseMixResults(String llmRaw) {
        if (llmRaw == null || llmRaw.isBlank()) {
            return List.of();
        }
        try {
            List<MixLLMResult> parsed = gson.fromJson(llmRaw, new TypeToken<List<MixLLMResult>>() {}.getType());
            return parsed == null ? List.of() : parsed;
        } catch (Exception ignore) {
            return List.of();
        }
    }

    private void appendTtsItems(List<Object> target, AtomicInteger indexGen, String agentId, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        List<String> fragments = splitSentence(text);
        for (String fragment : fragments) {
            List<String> audioChunks = synthesizeAudioChunks(fragment);
            if (audioChunks.isEmpty()) {
                target.add(buildTtsItem(indexGen.getAndIncrement(), agentId, fragment, ""));
                continue;
            }
            for (String chunk : audioChunks) {
                target.add(buildTtsItem(indexGen.getAndIncrement(), agentId, fragment, chunk));
            }
        }
    }

    private void appendMcpItems(List<Object> target, AtomicInteger indexGen, List<MixLLMEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (MixLLMEvent event : events) {
            if (event == null || event.eventType == null || event.eventType.isBlank()) {
                continue;
            }
            InstructionMcpItem item = new InstructionMcpItem();
            item.setType(Instruction.MCP);
            item.setIndex(indexGen.getAndIncrement());
            item.setRequestId(UUID.randomUUID().toString());
            item.setCode(200);
            item.setMessage("PENDING");
            item.setTarget("android_local");
            item.setDeviceId("");
            item.setCommandId(UUID.randomUUID().toString());
            item.setCommand(event.eventType);
            item.setParams(event.event == null ? Collections.emptyMap() : event.event);
            target.add(item);
        }
    }

    private InstructionTtsItem buildTtsItem(int index, String agentId, String text, String base64AudioStream) {
        InstructionTtsItem item = new InstructionTtsItem();
        item.setType(Instruction.TTS);
        item.setIndex(index);
        item.setAgentId(agentId);
        item.setText(text);
        item.setSeq(String.valueOf(index));
        item.setIsLast(false);
        item.setTimestamp(System.currentTimeMillis());
        item.setBase64AudioStream(base64AudioStream);
        return item;
    }

    /**
     * 同步等待一次句子 TTS 生成，返回音频分片列表。
     * 说明：该方法在编排线程阻塞等待，超时后返回已有分片（可能为空）。
     */
    private List<String> synthesizeAudioChunks(String sentence) {
        List<String> chunks = new ArrayList<>();
        CountDownLatch finish = new CountDownLatch(1);
        ttsServiceService.ttsSafelyStreamCall(sentence, new TTSStateCallback() {
            @Override
            public void recordDisposable(Disposable disposable) {
                // 由上层会话管理统一托管时可在此接入；当前无需额外处理。
            }

            @Override
            public void onStart(Subscription subscription) {
                // no-op
            }

            @Override
            public void onNext(String audioBase64Data) {
                if (audioBase64Data != null && !audioBase64Data.isBlank()) {
                    chunks.add(audioBase64Data);
                }
            }

            @Override
            public void onSingleComplete() {
                finish.countDown();
            }

            @Override
            public void onAllComplete() {
                // ttsSafelyStreamCall 主要使用 onSingleComplete 结束；这里作为冗余兜底。
                finish.countDown();
            }

            @Override
            public void haveNoSentence() {
                finish.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("TTS synthesize failed", throwable);
                finish.countDown();
            }
        });
        try {
            finish.await(8, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return chunks;
    }

    /** 基于中英文句末标点进行轻量句子切分。 */
    private List<String> splitSentence(String text) {
        List<String> result = new ArrayList<>();
        Matcher matcher = Pattern.compile("[^。！？!?\\n]+[。！？!?]?").matcher(text);
        while (matcher.find()) {
            String sentence = matcher.group().trim();
            if (!sentence.isEmpty()) {
                result.add(sentence);
            }
        }
        return result;
    }

    /** 下发 llm_start。 */
    private void sendLlmStart(WebSocketSession session, String agentId) {
        LlmStartResponse payload = new LlmStartResponse();
        payload.setAgentId(agentId);
        payload.setCode(200);
        payload.setMessage("OK");
        wsMessageSender.send(session, new ServerEvent(
                WsChannel.LLM.getValue(),
                WsEvent.LLM_START.getValue(),
                Map.of("payload", gson.toJson(payload))
        ));
    }

    /** 下发 llm_data（当前合并为单条文本）。 */
    private void sendLlmData(WebSocketSession session, String agentId, String text) {
        LlmDataResponse payload = new LlmDataResponse();
        payload.setAgentId(agentId);
        payload.setTextStream(text == null ? "" : text);
        payload.setSeq("0");
        payload.setIsLast(true);
        payload.setTimestamp(System.currentTimeMillis());
        wsMessageSender.send(session, new ServerEvent(
                WsChannel.LLM.getValue(),
                WsEvent.LLM_DATA.getValue(),
                Map.of("payload", gson.toJson(payload))
        ));
    }

    /** 下发 llm_end。 */
    private void sendLlmEnd(WebSocketSession session, String agentId) {
        LlmEndResponse payload = new LlmEndResponse();
        payload.setAgentId(agentId);
        payload.setCode(200);
        payload.setMessage("OK");
        wsMessageSender.send(session, new ServerEvent(
                WsChannel.LLM.getValue(),
                WsEvent.LLM_END.getValue(),
                Map.of("payload", gson.toJson(payload))
        ));
    }

    /** 下发 agent_start_reply，通知客户端进入“Agent 回复中”状态。 */
    private void sendAgentStartReply(WebSocketSession session) {
        SystemMessageResponse payload = new SystemMessageResponse();
        payload.setCode(200);
        payload.setMessage("OK");
        payload.setEvent("agent_start_reply");
        payload.setParam(Map.of());
        wsMessageSender.send(session, new ServerEvent(
                WsChannel.SYSTEM.getValue(),
                WsEvent.SYSTEM_MESSAGE.getValue(),
                Map.of("payload", gson.toJson(payload))
        ));
    }

    /** 下发 agent_end_reply，通知客户端恢复唤醒/录音。 */
    private void sendAgentEndReply(WebSocketSession session) {
        SystemMessageResponse payload = new SystemMessageResponse();
        payload.setCode(200);
        payload.setMessage("OK");
        payload.setEvent("agent_end_reply");
        payload.setParam(Map.of());
        wsMessageSender.send(session, new ServerEvent(
                WsChannel.SYSTEM.getValue(),
                WsEvent.SYSTEM_MESSAGE.getValue(),
                Map.of("payload", gson.toJson(payload))
        ));
    }

    /** String -> Long 的容错转换。 */
    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignore) {
            return null;
        }
    }
}
