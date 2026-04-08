package com.openapi.utils.pool;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自定义线程池，扩展 ThreadPoolExecutor，增加监控统计功能。
 * 设计意义：
 * 1. 统一命名线程，方便问题定位（通过 NamedThreadFactory）。
 * 2. 记录提交任务数（submittedCount）和完成任务数（completedCount），用于监控负载和积压。
 * 3. 提供 getPoolStatus() 方法，暴露当前线程池状态（活跃数、队列大小、完成率等），
 *    便于实现背压控制和动态告警。
 * 4. 可统一处理 beforeExecute / afterExecute，例如记录执行耗时、异常日志等（当前未实现，但预留扩展点）。
 */
@Slf4j
public class CustomThreadPool extends ThreadPoolExecutor {

    private final String poolName;
    private final AtomicLong submittedCount = new AtomicLong(0);
    private final AtomicLong completedCount = new AtomicLong(0);

    public CustomThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                            TimeUnit unit, BlockingQueue<Runnable> workQueue,
                            RejectedExecutionHandler handler, String poolName) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                new NamedThreadFactory(poolName), handler);
        this.poolName = poolName;
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        // 每提交一个任务，计数加1（注意：此处统计的是即将执行的任务，实际提交计数更准确的做法是在 execute() 中加，
        // 但 execute 无法直接覆盖，所以这里作为近似监控值）
        submittedCount.incrementAndGet();
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        completedCount.incrementAndGet();
        // 可在此记录任务执行异常
        if (t != null) {
            log.warn("Task execution failed in pool {}: {}", poolName, t.getMessage());
        }
    }

    public long getSubmittedCount() {
        return submittedCount.get();
    }

    public long getCompletedCount() {
        return completedCount.get();
    }

    /**
     * 获取线程池当前状态快照，用于监控和告警。
     */
    public PoolStatus getPoolStatus() {
        return new PoolStatus(poolName, getCorePoolSize(), getMaximumPoolSize(),
                getActiveCount(), getPoolSize(), getQueue().size(),
                getCompletedTaskCount(), getSubmittedCount());
    }

    /**
     * 线程池状态 DTO
     */
    public static class PoolStatus {
        private final String poolName;
        private final int corePoolSize;
        private final int maxPoolSize;
        private final int activeCount;
        private final int poolSize;
        private final int queueSize;
        private final long completedTaskCount;
        private final long submittedCount;

        // 构造器、getter 省略（实际代码应补全）
        public PoolStatus(String poolName, int corePoolSize, int maxPoolSize,
                          int activeCount, int poolSize, int queueSize,
                          long completedTaskCount, long submittedCount) {
            this.poolName = poolName;
            this.corePoolSize = corePoolSize;
            this.maxPoolSize = maxPoolSize;
            this.activeCount = activeCount;
            this.poolSize = poolSize;
            this.queueSize = queueSize;
            this.completedTaskCount = completedTaskCount;
            this.submittedCount = submittedCount;
        }

        // getters...
    }

    /**
     * 线程工厂，为每个线程池内的线程统一命名，便于日志追踪。
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        public NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            if (t.isDaemon()) t.setDaemon(false);   // 确保不是守护线程，避免 JVM 提前退出
            if (t.getPriority() != Thread.NORM_PRIORITY) t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}