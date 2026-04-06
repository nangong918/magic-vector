package com.openapi.config;

import com.openapi.utils.pool.CustomThreadPool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.*;

/**
 * 线程池配置类，严格按照设计文档定义所有业务线程池。
 * 每个线程池的容量、拒绝策略、队列大小均与文档一致。
 */
@Configuration
public class ThreadPoolConfig {

    @Bean("taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("AudioChat-");
        executor.initialize();
        return executor;
    }

    @Bean("messageExecutor")
    public ThreadPoolTaskExecutor messageExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("Message-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 通用业务线程池：处理连接管理、Agent同步、聊天、状态上报、系统消息等。
     * 核心10，最大20，队列1000，拒绝策略 CallerRunsPolicy（减缓生产者速度）。
     */
    @Bean("businessExecutor")
    public ExecutorService businessExecutor() {
        return new CustomThreadPool(
                10, 20, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy(),
                "business-%d"
        );
    }

    /**
     * 控制命令线程池：处理 control 通道消息，高优先级，快速响应。
     * 核心5，最大10，队列200，拒绝策略 CallerRunsPolicy。
     */
    @Bean("controlExecutor")
    public ExecutorService controlExecutor() {
        return new CustomThreadPool(
                5, 10, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),
                new ThreadPoolExecutor.CallerRunsPolicy(),
                "control-%d"
        );
    }

    /**
     * STT 音频处理线程池：音频流碎片处理，耗时较长，独立隔离。
     * 核心2，最大4，队列100，拒绝策略 DiscardOldestPolicy（丢弃最老任务，优先处理最新音频）。
     */
    @Bean("sttExecutor")
    public ExecutorService sttExecutor() {
        return new CustomThreadPool(
                2, 4, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadPoolExecutor.DiscardOldestPolicy(),
                "stt-%d"
        );
    }

    /**
     * LLM 调用线程池：调用外部大模型服务，耗时最长。
     * 核心3，最大6，队列200，拒绝策略 CallerRunsPolicy。
     */
    @Bean("llmExecutor")
    public ExecutorService llmExecutor() {
        return new CustomThreadPool(
                3, 6, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),
                new ThreadPoolExecutor.CallerRunsPolicy(),
                "llm-%d"
        );
    }

    /**
     * TTS 合成线程池：文本转语音，耗时中等。
     * 核心2，最大4，队列100，拒绝策略 DiscardOldestPolicy。
     */
    @Bean("ttsExecutor")
    public ExecutorService ttsExecutor() {
        return new CustomThreadPool(
                2, 4, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadPoolExecutor.DiscardOldestPolicy(),
                "tts-%d"
        );
    }

    /**
     * VL 视觉理解线程池：视频帧处理，资源消耗大，并发度低。
     * 核心1，最大2，队列50，拒绝策略 DiscardOldestPolicy。
     */
    @Bean("vlExecutor")
    public ExecutorService vlExecutor() {
        return new CustomThreadPool(
                1, 2, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(50),
                new ThreadPoolExecutor.DiscardOldestPolicy(),
                "vl-%d"
        );
    }

    /**
     * 指令批量追踪线程池：处理 instruction_list 结果回执，轻量但需要顺序保证。
     * 核心2，最大4，队列200，拒绝策略 CallerRunsPolicy。
     */
    @Bean("instructionListExecutor")
    public ExecutorService instructionListExecutor() {
        return new CustomThreadPool(
                2, 4, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),
                new ThreadPoolExecutor.CallerRunsPolicy(),
                "inst-list-%d"
        );
    }

    /**
     * 监控定时任务线程池：单线程，用于定期采集各线程池状态。
     */
    @Bean("monitoringExecutor")
    public ScheduledExecutorService monitoringExecutor() {
        return new ScheduledThreadPoolExecutor(1, r ->
                new Thread(r, "monitor-%d"));
    }
}
