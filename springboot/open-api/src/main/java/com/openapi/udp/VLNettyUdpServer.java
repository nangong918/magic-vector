package com.openapi.udp;

import com.openapi.component.manager.realTimeChat.VLManager;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

/**
 * @author 13225
 * @date 2025/11/13 14:40
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VLNettyUdpServer {

    private EventLoopGroup workerGroup;
    private Channel channel;
    private final VideoSessionManager videoSessionManager;

    @EventListener(ApplicationReadyEvent.class)
    public void startServer() {
        new Thread(() -> {
            try {
                workerGroup = new NioEventLoopGroup(4); // 固定线程池 4个IO线程

                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(workerGroup)
                        .channel(NioDatagramChannel.class)
                        .handler(new ChannelInitializer<NioDatagramChannel>() {
                            @Override
                            protected void initChannel(NioDatagramChannel ch) {
                                ChannelPipeline pipeline = ch.pipeline();
                                pipeline.addLast(new UdpPacketHandler(videoSessionManager)); // 2. 事件驱动
                            }
                        })
                        .option(ChannelOption.SO_RCVBUF, 1024 * 1024) // 1MB接收缓冲区
                        .option(ChannelOption.SO_SNDBUF, 1024 * 1024) // 1MB发送缓冲区
                        .option(ChannelOption.SO_BROADCAST, false);

                channel = bootstrap.bind(45000).sync().channel();
                log.info("Netty UDP服务器启动成功，端口: 45000");

                channel.closeFuture().await();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("UDP服务器被中断");
            } catch (Exception e) {
                log.error("启动Netty UDP服务器失败", e);
            }
        }, "Netty-UDP-Server").start();
    }

    @PreDestroy
    public void stopServer() {
        log.info("正在关闭Netty UDP服务器...");

        if (channel != null) {
            channel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }

        log.info("Netty UDP服务器已关闭");
    }
}

