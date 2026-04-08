package com.openapi.connect.websocket.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.openapi.connect.websocket.manager.ConnectionManager;
import com.openapi.connect.websocket.manager.WsAgentSyncManager;
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
import java.util.HashMap;
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
     * 语音主链路编排（与 {@link WsAgentSyncManager} 配合）：
     * STT 最终文本落库(user，仅存语音识别内容) → 将 STT+可选 VL 拼成 LLM 用户侧上下文 →
     * LLM → 解析为 tts_event / mcp_event 逐条下发（客户端按 instructionTiming+index 排序执行）。
     */
    public void handleSyncedUserTurn(WebSocketSession session, String agentId, String sttFinalText, String vlText) {
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

        String sttStored = sttFinalText == null ? "" : sttFinalText.trim();
        if (!sttStored.isEmpty()) {
            chatMessageService.insertOne(agentIdLong, sttStored, true, userIdLong);
        }

        String llmUserPayload = buildLlmUserPayload(sttStored, vlText);

        sendLlmStart(session, agentId);

        String llmRaw = callLlm(agentId, userId, llmUserPayload);
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
        bindRequestId(instructionItems, requestId);
        trackerManager.register(requestId, session, userId, instructionItems.size());
        sendInstructionEvents(session, instructionItems);
    }

    /** STT 单路（无 VL 等待）的兼容入口，测试或旧调用方可直接用。 */
    public void handleSttFinal(WebSocketSession session, String agentId, String sttFinalText) {
        handleSyncedUserTurn(session, agentId, sttFinalText, "");
    }

    /**
     * LLM 用户消息：有 VL 时在 STT 后附加 {@code [视觉]} 段；历史库仍只存纯 STT。
     */
    private static String buildLlmUserPayload(String stt, String vl) {
        String v = vl == null ? "" : vl.trim();
        String s = stt == null ? "" : stt.trim();
        if (v.isEmpty()) {
            return s;
        }
        if (s.isEmpty()) {
            return "[视觉]\n" + v;
        }
        return s + "\n[视觉]\n" + v;
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
     * 构建事件流（tts_event/mcp_event）：
     * - 优先按 MixLLMResult 顺序分配 instructionTiming（结果项序号即阶段号）；
     * - 解析失败时回退为 timing=0 的纯 TTS 事件。
     */
    private List<Object> buildInstructions(String agentId, String llmRaw, String chatText) {
        List<MixLLMResult> parsed = parseMixResults(llmRaw);
        AtomicInteger indexGen = new AtomicInteger(0);
        List<Object> result = new ArrayList<>();

        if (!parsed.isEmpty()) {
            int instructionTiming = 0;
            for (MixLLMResult item : parsed) {
                if (item == null) {
                    continue;
                }
                int timing = item.instructionTiming == null ? instructionTiming : item.instructionTiming;
                appendTtsItems(result, indexGen, agentId, item.chatSentence, timing);
                appendMcpItems(result, indexGen, item.eventList, timing);
                instructionTiming++;
            }
        } else if (appendFromTimingBlocks(result, indexGen, agentId, llmRaw)) {
            // 已按分组 JSON（含 instructionTiming）完成解析
        } else {
            appendTtsItems(result, indexGen, agentId, chatText, 0);
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

    private void appendTtsItems(List<Object> target, AtomicInteger indexGen, String agentId, String text, int instructionTiming) {
        if (text == null || text.isBlank()) {
            return;
        }
        List<String> fragments = splitSentence(text);
        for (String fragment : fragments) {
            List<String> audioChunks = synthesizeAudioChunks(fragment);
            if (audioChunks.isEmpty()) {
                target.add(buildTtsItem(indexGen.getAndIncrement(), instructionTiming, agentId, fragment, ""));
                continue;
            }
            for (String chunk : audioChunks) {
                target.add(buildTtsItem(indexGen.getAndIncrement(), instructionTiming, agentId, fragment, chunk));
            }
        }
    }

    private void appendMcpItems(List<Object> target, AtomicInteger indexGen, List<MixLLMEvent> events, int instructionTiming) {
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
            item.setInstructionTiming(instructionTiming);
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

    /**
     * 兼容 LLM 直接返回的分组结构：
     * [
     *   [{"text":"...","instructionTiming":0}],
     *   [{"command":"GPIO_SET","target":"rk","params":{"k":"v"},"instructionTiming":1}]
     * ]
     */
    private boolean appendFromTimingBlocks(List<Object> target, AtomicInteger indexGen, String agentId, String llmRaw) {
        if (llmRaw == null || llmRaw.isBlank()) {
            return false;
        }
        try {
            JsonElement root = gson.fromJson(llmRaw, JsonElement.class);
            if (root == null || !root.isJsonArray()) {
                return false;
            }
            JsonArray blocks = root.getAsJsonArray();
            boolean appended = false;
            for (int blockIdx = 0; blockIdx < blocks.size(); blockIdx++) {
                JsonElement blockEl = blocks.get(blockIdx);
                if (blockEl == null || !blockEl.isJsonArray()) {
                    continue;
                }
                JsonArray block = blockEl.getAsJsonArray();
                for (JsonElement itemEl : block) {
                    if (itemEl == null || !itemEl.isJsonObject()) {
                        continue;
                    }
                    JsonObject item = itemEl.getAsJsonObject();
                    int timing = readInt(item, "instructionTiming", blockIdx);

                    String text = readString(item, "text");
                    if (!text.isBlank()) {
                        appendTtsItems(target, indexGen, agentId, text, timing);
                        appended = true;
                    }

                    String command = readString(item, "command");
                    if (!command.isBlank()) {
                        InstructionMcpItem mcp = new InstructionMcpItem();
                        mcp.setType(Instruction.MCP);
                        mcp.setIndex(indexGen.getAndIncrement());
                        mcp.setInstructionTiming(timing);
                        mcp.setCode(200);
                        mcp.setMessage("PENDING");
                        String targetRaw = readString(item, "target");
                        mcp.setTarget("rk".equalsIgnoreCase(targetRaw) ? "rk_device" : "android_local");
                        mcp.setDeviceId(readString(item, "deviceId"));
                        mcp.setCommandId(UUID.randomUUID().toString());
                        mcp.setCommand(command);
                        mcp.setParams(readStringMap(item, "params"));
                        target.add(mcp);
                        appended = true;
                    }
                }
            }
            return appended;
        } catch (Exception ignore) {
            return false;
        }
    }

    private static String readString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return "";
        }
        try {
            return obj.get(key).getAsString().trim();
        } catch (Exception ignore) {
            return "";
        }
    }

    private static int readInt(JsonObject obj, String key, int defaultVal) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return defaultVal;
        }
        try {
            return obj.get(key).getAsInt();
        } catch (Exception ignore) {
            return defaultVal;
        }
    }

    private Map<String, String> readStringMap(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull() || !obj.get(key).isJsonObject()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<>();
        JsonObject raw = obj.getAsJsonObject(key);
        for (Map.Entry<String, JsonElement> e : raw.entrySet()) {
            if (e.getValue() == null || e.getValue().isJsonNull()) {
                continue;
            }
            result.put(e.getKey(), e.getValue().getAsString());
        }
        return result;
    }

    private InstructionTtsItem buildTtsItem(int index, int instructionTiming, String agentId, String text, String base64AudioStream) {
        InstructionTtsItem item = new InstructionTtsItem();
        item.setType(Instruction.TTS);
        item.setIndex(index);
        item.setInstructionTiming(instructionTiming);
        item.setAgentId(agentId);
        item.setText(text);
        item.setSeq(String.valueOf(index));
        item.setIsLast(false);
        item.setTimestamp(System.currentTimeMillis());
        item.setBase64AudioStream(base64AudioStream);
        return item;
    }

    /** 统一回填 requestId 到每条事件。 */
    private void bindRequestId(List<Object> items, String requestId) {
        for (Object item : items) {
            if (item instanceof InstructionTtsItem tts) {
                tts.setRequestId(requestId);
            } else if (item instanceof InstructionMcpItem mcp) {
                mcp.setRequestId(requestId);
            }
        }
    }

    /** 逐条下发 tts_event / mcp_event，客户端按 instructionTiming+index 排序执行。 */
    private void sendInstructionEvents(WebSocketSession session, List<Object> instructionItems) {
        for (Object item : instructionItems) {
            if (item instanceof InstructionTtsItem tts) {
                wsMessageSender.send(session, new ServerEvent(
                        WsChannel.TTS.getValue(),
                        WsEvent.TTS_EVENT.getValue(),
                        toFlatTtsEventData(tts)
                ));
            } else if (item instanceof InstructionMcpItem mcp) {
                wsMessageSender.send(session, new ServerEvent(
                        WsChannel.CONTROL.getValue(),
                        WsEvent.MCP_EVENT.getValue(),
                        toFlatMcpEventData(mcp)
                ));
            }
        }
    }

    /** tts_event 扁平 data（去掉 payload 包装）。 */
    private Map<String, String> toFlatTtsEventData(InstructionTtsItem tts) {
        Map<String, String> data = new HashMap<>();
        data.put("requestId", nullToEmpty(tts.getRequestId()));
        data.put("agentId", nullToEmpty(tts.getAgentId()));
        data.put("type", tts.getType() == null ? "" : tts.getType().getValue());
        data.put("index", String.valueOf(tts.getIndex()));
        data.put("instructionTiming", String.valueOf(tts.getInstructionTiming()));
        data.put("text", nullToEmpty(tts.getText()));
        data.put("seq", nullToEmpty(tts.getSeq()));
        data.put("isLast", String.valueOf(Boolean.TRUE.equals(tts.getIsLast())));
        data.put("timestamp", String.valueOf(tts.getTimestamp()));
        data.put("base64AudioStream", nullToEmpty(tts.getBase64AudioStream()));
        return data;
    }

    /** mcp_event 扁平 data（去掉 payload 包装）。 */
    private Map<String, String> toFlatMcpEventData(InstructionMcpItem mcp) {
        Map<String, String> data = new HashMap<>();
        data.put("requestId", nullToEmpty(mcp.getRequestId()));
        data.put("type", mcp.getType() == null ? "" : mcp.getType().getValue());
        data.put("index", String.valueOf(mcp.getIndex()));
        data.put("instructionTiming", String.valueOf(mcp.getInstructionTiming()));
        data.put("target", nullToEmpty(mcp.getTarget()));
        data.put("deviceId", nullToEmpty(mcp.getDeviceId()));
        data.put("commandId", nullToEmpty(mcp.getCommandId()));
        data.put("command", nullToEmpty(mcp.getCommand()));
        data.put("params", gson.toJson(mcp.getParams() == null ? Collections.emptyMap() : mcp.getParams()));
        data.put("code", String.valueOf(mcp.getCode()));
        data.put("message", nullToEmpty(mcp.getMessage()));
        return data;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
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
