package com.openapi.udp;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.openapi.domain.dto.udp.VideoUdpPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author 13225
 * @date 2025/11/13 14:47
 */
@Slf4j
@ChannelHandler.Sharable
@Component
public class UdpPacketHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private final VideoSessionManager videoSessionManager;
    // 业务处理线程池
    private final ExecutorService businessExecutor;

    public UdpPacketHandler(VideoSessionManager videoSessionManager) {
        this.videoSessionManager = videoSessionManager;
        // 业务处理线程池，与Netty IO线程分离
        this.businessExecutor = new ThreadPoolExecutor(
                10, 50, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                new ThreadFactoryBuilder().setNameFormat("udp-business-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket packet) throws Exception {
        // 提交到业务线程池处理，不阻塞Netty IO线程
        businessExecutor.execute(() -> processPacket(packet));
    }

    private void processPacket(DatagramPacket packet) {
        try {
            ByteBuf content = packet.content();
            byte[] data = new byte[content.readableBytes()]; // 4. 立即复制数据，避免竞争
            content.readBytes(data);

            String json = new String(data, StandardCharsets.UTF_8);
            VideoUdpPacket videoPacket = VideoUdpPacket.fromJson(json);

            // 处理视频数据包
            videoSessionManager.processVideoPacket(videoPacket);

            // 记录接收统计
            log.debug("收到UDP数据包 - 用户: {}, 会话: {}, 分片: {}/{}",
                    videoPacket.getUserId(), videoPacket.getSessionId(),
                    videoPacket.getChunkIndex() + 1, videoPacket.getTotalChunks());

        } catch (Exception e) {
            log.error("处理UDP数据包异常", e);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof IOException) {
            log.debug("UDP连接异常: {}", cause.getMessage());
        } else {
            log.error("UDP处理器异常", cause);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @PreDestroy
    public void shutdown() {
        if (businessExecutor != null) {
            businessExecutor.shutdown();
            try {
                if (!businessExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    businessExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                businessExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
