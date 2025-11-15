package com.openapi.connect.udp;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.openapi.domain.constant.UdpDataType;
import com.openapi.domain.dto.udp.VideoUdpPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import lombok.NonNull;
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
        // 在IO线程中立即复制数据，然后提交到业务线程池
        ByteBuf content = packet.content();

        // 方法1：立即复制数据到字节数组
        byte[] data = new byte[content.readableBytes()];
        content.readBytes(data); // 这里会移动readIndex，但不会释放原始buffer

        // 提交复制的数据到线程池
        businessExecutor.execute(() -> processPacket(data));

        // 或者使用方法2：增加引用计数并传递ByteBuf（更复杂）
        // businessExecutor.execute(() -> processPacketWithRefCount(packet.retain()));
    }

    private void processPacket(byte[] data) {
        if (data == null || data.length == 0){
            log.warn("收到空数据包");
            return;
        }
        try {
            // 1. 解析数据类型（第一个字节）
            byte typeByte = data[0];
            UdpDataType dataType;
            try {
                dataType = UdpDataType.fromByte(typeByte);
            } catch (IllegalArgumentException e) {
                log.warn("未知的数据类型: {}", typeByte);
                return;
            }

            switch (dataType) {
                case VIDEO -> {
                    processVideoPacket(data);
                }
                case AUDIO -> {
                    log.debug("收到音频数据包，暂未实现");
                }
                case CONTROL -> {
                    log.debug("收到控制指令数据包，暂未实现");
                }
                case HEARTBEAT -> {
                    log.debug("收到心跳包，暂未实现");
                }
            }
        } catch (Exception e) {
            log.error("处理UDP数据包异常", e);
        }
    }

    private void processVideoPacket(byte @NonNull [] bytes) {
        try {
            // 解析二进制协议
            VideoUdpPacket packet = VideoUdpPacket.fromBytes(bytes);

            // 处理视频数据包
            videoSessionManager.processVideoPacket(packet);

            log.debug("收到视频数据包 - 用户: {}, 分片: {}/{}",
                    packet.getUserId(),
                    packet.getChunkIndex() + 1, packet.getTotalChunks());

        } catch (Exception e) {
            log.error("处理视频数据包异常", e);
        }
    }

    /**
     * 处理复制的数据
     */
    private void processPacketJSON(byte[] data) {
        try {
            String json = new String(data, StandardCharsets.UTF_8);
            System.out.println("收到UDP数据包: " + json);
            VideoUdpPacket videoPacket = VideoUdpPacket.fromJson(json);

            // 处理视频数据包
            videoSessionManager.processVideoPacket(videoPacket);

            // 记录接收统计
            log.debug("收到UDP数据包 - 用户: {}, 分片: {}/{}",
                    videoPacket.getUserId(),
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
