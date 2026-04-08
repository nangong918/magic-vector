package com.openapi.connect.websocket.manager;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * WS 语音轮次同步管理器（只做 STT 与可选 VL 的汇合，不做 LLM/TTS 执行）。
 *
 * <p>设计目标：
 * 1) 把“一回合是否需要等 VL、VL 文本累加、何时放行 LLM”集中在一个地方；
 * 2) 支持两种触发方式：stt_start.needVl 显式指定 或 vl_start 事件声明；
 * 3) 发生断连时可按 session 统一清理，避免状态泄漏。</p>
 *
 * <p>关键时序（单轮）：
 * stt_start -> (可选 vl_start / VL pipeline) -> stt_end -> STT onRecognitionComplete
 * -> finishSttAndSyncVl() -> 返回 {sttText, vlText} 供上层拼 LLM 输入。</p>
 */
@Slf4j
@Component
public class WsAgentSyncManager {

    /** 与 AgentChat 设计一致：停止录音后等待 VL 的上限。 */
    private static final long VL_WAIT_MS = 5000L;

    /**
     * 轮次状态容器。
     * key: webSocketSessionId:agentId
     * value: 本 agent 在该 WS 连接上的“当前语音轮次”状态。
     */
    private final ConcurrentHashMap<String, VoiceTurn> turns = new ConcurrentHashMap<>();

    private static String key(String sessionId, String agentId) {
        return sessionId + ":" + agentId;
    }

    /** 连接关闭时移除该 WS 下所有轮次状态，避免泄漏。 */
    public void removeAllForWebSocketSession(String webSocketSessionId) {
        turns.keySet().removeIf(k -> k.startsWith(webSocketSessionId + ":"));
    }

    /**
     * 客户端发起 stt_start：
     * - 打开本轮 sttTurnOpen；
     * - 决定本轮是否等待 VL（needVlOverride 优先，否则回退 pendingVlIntent）；
     * - 清空上轮 VL 文本并重建/清空 latch。
     *
     * @param needVlOverride 本轮是否等待 VL，可空
     *                      - true: 一定等待 VL（或超时）
     *                      - false: 本轮不等待 VL（即使之前有 pending）
     *                      - null: 兼容模式，按 pendingVlIntent 判定
     */
    public void onSttStart(String webSocketSessionId, String agentId, Boolean needVlOverride) {
        if (agentId == null || agentId.isBlank()) {
            return;
        }
        turns.compute(key(webSocketSessionId, agentId), (k, v) -> {
            VoiceTurn t = v == null ? new VoiceTurn() : v;
            synchronized (t) {
                t.sttTurnOpen = true;
                if (needVlOverride != null) {
                    t.vlWait = needVlOverride;
                } else {
                    t.vlWait = t.pendingVlIntent;
                }
                t.pendingVlIntent = false;
                t.vlText.setLength(0);
                t.vlLatch = t.vlWait ? new CountDownLatch(1) : null;
            }
            return t;
        });
    }

    /**
     * 兼容旧调用：未显式传 needVl 时，按 pendingVlIntent 判定。
     */
    public void onSttStart(String webSocketSessionId, String agentId) {
        onSttStart(webSocketSessionId, agentId, null);
    }

    /**
     * 客户端发起 vl_start：
     * - 先记 pendingVlIntent=true（解决“vl_start 早于 stt_start”的乱序）；
     * - 若当前 stt 已开启，则立刻把本轮切到 vlWait=true 并初始化 latch。
     */
    public void onClientVlStart(String webSocketSessionId, String agentId) {
        if (agentId == null || agentId.isBlank()) {
            return;
        }
        turns.compute(key(webSocketSessionId, agentId), (k, v) -> {
            VoiceTurn t = v == null ? new VoiceTurn() : v;
            synchronized (t) {
                t.pendingVlIntent = true;
                if (t.sttTurnOpen) {
                    t.vlWait = true;
                    t.vlText.setLength(0);
                    t.vlLatch = new CountDownLatch(1);
                }
            }
            return t;
        });
    }

    /**
     * VL 管道**整段结果一次性**回调时使用，等价于 {@code notifyVlPipelineComplete(sessionId, agentId, content, true)}。
     */
    public void notifyVlPipelineComplete(String webSocketSessionId, String agentId, String content) {
        notifyVlPipelineComplete(webSocketSessionId, agentId, content, true);
    }

    /**
     * UDP/RTMP → VL 服务等多段输出：可多次 {@code endOfRound=false} 追加文本，最后一次 {@code endOfRound=true}
     * 再唤醒正在 {@link #finishSttAndSyncVl} 中等待的线程；仅当本轮 {@code vlWait=true} 时生效。
     *
     * <p>注意：这里不区分“中间段/最终段”的业务语义，只按 endOfRound 控制是否放行。</p>
     */
    public void notifyVlPipelineComplete(
            String webSocketSessionId,
            String agentId,
            String content,
            boolean endOfRound
    ) {
        if (agentId == null || agentId.isBlank()) {
            return;
        }
        VoiceTurn t = turns.get(key(webSocketSessionId, agentId));
        if (t == null) {
            return;
        }
        synchronized (t) {
            if (!t.vlWait) {
                return;
            }
            if (content != null && !content.isBlank()) {
                if (!t.vlText.isEmpty()) {
                    t.vlText.append('\n');
                }
                t.vlText.append(content.trim());
            }
            if (endOfRound) {
                CountDownLatch latch = t.vlLatch;
                if (latch != null) {
                    latch.countDown();
                }
            }
        }
    }

    /**
     * STT 最终结果已就绪：若本回合需要 VL，则阻塞等待管道 {@code endOfRound=true} 的一次通知或超时；
     * 返回 STT 原文与 VL 文本（可能为空），供上层拼 LLM 输入。
     *
     * <p>并发约束：在同一个 VoiceTurn 上加锁，保证 stt_end 收尾与 VL 回调不会并发破坏缓冲。</p>
     */
    public UtteranceSnapshot finishSttAndSyncVl(String webSocketSessionId, String agentId, String sttFinal) {
        String stt = sttFinal == null ? "" : sttFinal.trim();
        String vl = "";
        VoiceTurn t = turns.get(key(webSocketSessionId, agentId));
        if (t == null) {
            return new UtteranceSnapshot(stt, vl);
        }
        synchronized (t) {
            t.sttTurnOpen = false;
            if (t.vlWait && t.vlLatch != null) {
                try {
                    boolean done = t.vlLatch.await(VL_WAIT_MS, TimeUnit.MILLISECONDS);
                    if (!done) {
                        log.warn("VL wait timeout sessionId={} agentId={} ms={}", webSocketSessionId, agentId, VL_WAIT_MS);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            vl = t.vlText.toString().trim();
        }
        return new UtteranceSnapshot(stt, vl);
    }

    @Getter
    public static final class UtteranceSnapshot {
        /** 本轮 STT 最终文本。 */
        private final String sttText;
        /** 本轮汇总后的 VL 文本（可能为空字符串）。 */
        private final String vlText;

        public UtteranceSnapshot(String sttText, String vlText) {
            this.sttText = sttText == null ? "" : sttText;
            this.vlText = vlText == null ? "" : vlText;
        }
    }

    static final class VoiceTurn {
        /** STT 是否处于“本轮已开始、尚未收尾”的窗口。 */
        boolean sttTurnOpen;
        /** 当 vl_start 早于 stt_start 时的意图缓存。 */
        boolean pendingVlIntent;
        /** 本轮是否需要等待 VL 完成信号。 */
        boolean vlWait;
        /** VL 文本缓冲（可累计多段）。 */
        final StringBuilder vlText = new StringBuilder();
        /** 仅在 vlWait=true 时创建；用于 STT 收尾线程等待 VL 放行。 */
        CountDownLatch vlLatch;
    }
}
