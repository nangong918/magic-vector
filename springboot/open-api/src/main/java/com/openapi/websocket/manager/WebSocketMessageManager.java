package com.openapi.websocket.manager;

import com.openapi.component.manager.RealtimeChatContextManager;
import com.openapi.domain.ao.MessageTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author 13225
 * @date 2025/11/4 15:29
 * 任务触发式消息管理者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketMessageManager {
    private final BlockingQueue<MessageTask> messageQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private final ConcurrentMap<String, RealtimeChatContextManager> realtimeChatContextManagerMap;
    private final ThreadPoolTaskExecutor messageExecutor;
    /**
     * 提交消息发送任务
     * @param agentId agent会话Id
     * @param message 消息内容
     */
    public void submitMessage(String agentId, String message/*, SendCallback callback*/) {
        if (StringUtils.hasText(agentId) && StringUtils.hasText(message)) {
            boolean addResult = messageQueue.offer(new MessageTask(agentId, message));
            if (!addResult){
                log.warn("websocket::submitMessage 添加消息失败");
            }
            scheduleProcessing();
        }
    }

    /**
     * 调度消息处理
     */
    private void scheduleProcessing() {
        // 如果当前没有在处理，就提交处理任务到线程池
        if (!messageQueue.isEmpty() && isProcessing.compareAndSet(false, true)) {
            messageExecutor.execute(this::processMessages);
            log.debug("提交消息处理任务到线程池");
        }
    }

    /**
     * 处理消息的核心方法
     */
    private void processMessages() {
        final String threadName = Thread.currentThread().getName();
        try {
            log.debug("开始处理消息队列::线程名称: {}, 消息数量剩余: {}", threadName, messageQueue.size());

            // 持续处理直到队列为空
            while (!messageQueue.isEmpty()) {
                MessageTask task = messageQueue.poll(); // 非阻塞获取
                if (task != null) {
                    sendMessageInternal(task);
                }

                // 短暂休眠避免CPU空转
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

        } catch (Exception e) {
            log.error("处理消息时发生异常: {}", threadName, e);
        } finally {
            isProcessing.set(false);
            log.debug("消息处理完成: {}, 剩余消息: {}", threadName, messageQueue.size());

            // 检查是否有新消息到达，如果有则重新调度
            if (!messageQueue.isEmpty()) {
                scheduleProcessing();
            }
        }
    }

    /**
     * 内部发送消息方法
     */
    private void sendMessageInternal(MessageTask task) {
        final String agentId = task.agentId;
        final String message = task.message;
        if (!StringUtils.hasText(agentId) || !StringUtils.hasText(message)){
            log.warn("发送消息失败，agentId：{}，message size：{}", agentId, message == null ? 0 : message.length());
            return;
        }
        try {
            final WebSocketSession session = Optional.ofNullable(realtimeChatContextManagerMap.get(agentId))
                    .map(it -> it.session)
                    .orElse(null);
            if (session == null || !session.isOpen()){
                log.warn("发送消息异常：session是空或者关闭");
                return;
            }
            synchronized (session) {
                session.sendMessage(new TextMessage(task.message));
            }
        } catch (IOException e) {
            log.error("发送WebSocket消息时发生IO异常, agentId: {}", agentId, e);
        } catch (Exception e) {
            log.error("发送WebSocket消息时发生异常, agentId: {}", agentId, e);
        }
    }

    /**
     * 获取队列中待处理的消息数量
     */
    public int getPendingMessageCount() {
        return messageQueue.size();
    }

    /**
     * 清空消息队列
     */
    public void clearQueue() {
        messageQueue.clear();
    }


}
