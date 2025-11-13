package com.openapi.udp;

import com.openapi.domain.dto.udp.VideoUdpPacket;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 13225
 * @date 2025/11/13 12:54
 */
@Component
public class VLUdpServer {

    private final Map<String, VideoSession> sessions = new ConcurrentHashMap<>();

    @PostConstruct
    public void startServer() {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(45000)) {
                byte[] buffer = new byte[1024 * 20];

                System.out.println("UDP服务器启动 - 无确认模式");

                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    // 异步处理，不阻塞接收
                    CompletableFuture.runAsync(() -> processPacket(packet));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void processPacket(DatagramPacket udpPacket) {
        try {
            String json = new String(udpPacket.getData(), 0, udpPacket.getLength(), StandardCharsets.UTF_8);
            VideoUdpPacket packet = VideoUdpPacket.fromJson(json);

            String sessionKey = packet.getUserId() + ":" + packet.getAgentId() + ":" + packet.getSessionId();

            VideoSession session = sessions.computeIfAbsent(sessionKey,
                    k -> new VideoSession(packet.getUserId(), packet.getAgentId(),
                            packet.getSessionId(), packet.getTotalChunks()));

            session.addChunk(packet.getChunkIndex(), packet.getData());

            // 如果收集完成，处理视频
            if (session.isComplete()) {
                processCompleteVideo(session);
                sessions.remove(sessionKey);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processCompleteVideo(VideoSession session) {
        String completeBase64 = session.getCompleteData();
        System.out.println("收到完整视频: " + session.getSessionId() +
                ", 用户: " + session.getUserId());

    }
}
