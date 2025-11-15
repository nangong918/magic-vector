package com.openapi.domain.dto.udp;

import com.alibaba.fastjson.JSON;
import com.openapi.utils.ByteUtils;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author 13225
 * @date 2025/11/13 12:53
 * 视频数据包, udp socket需要用一个字节来标注是哪种类型的数据
 * [数据类型:1字节][userId长度字段:1字节][agentId长度字段:1字节][chunkIndex:4字节][totalChunks:4字节][CRC16校验:2字节][userId:20字节][agentId:20字节]
 */
@Data
public class VideoUdpPacket {
    private String userId;        // 用户ID
    private String agentId;       // 代理ID
//    private String sessionId;     // 会话ID
    private int chunkIndex;       // 分块索引
    private int totalChunks;      // 总块数
//    private long timestamp;       // 时间戳
    private String data;          // 实际数据

    public String toJson() {
        return JSON.toJSONString(this);
    }

    public static VideoUdpPacket fromJson(String json) {
        return JSON.parseObject(json, VideoUdpPacket.class);
    }

    /**
     * [数据类型:1字节][userId长度字段:1字节][agentId长度字段:1字节][chunkIndex:4字节][totalChunks:4字节][CRC16校验:2字节][userId:20字节][agentId:20字节]
     * head: 1 + 1 + 1 + 4 + 4 + 2 = 13
     * all: 13 + 20 + 20 = 53
     * @param bytes 二进制数据
     * @return  视频数据包
     */
    public static VideoUdpPacket fromBytes(byte[] bytes) {
        if (bytes.length <= 13) {
            throw new IllegalArgumentException("数据包过短: " + bytes.length + " bytes");
        }

        int offset = 0;

        // 1. 数据类型 (已在前面的processPacket中验证)
        offset += 1; // 跳过数据类型字节

        // 2. 用户ID长度
        int userIdLen = bytes[offset++] & 0xFF;

        // 3. 代理ID长度
        int agentIdLen = bytes[offset++] & 0xFF;

        // 4. 分片索引
        int chunkIndex = ByteUtils.readInt(bytes, offset);
        offset += 4;

        // 5. 总分片数
        int totalChunks = ByteUtils.readInt(bytes, offset);
        offset += 4;

        // 6. CRC16校验
        int crcOffset = offset;
        short receivedCRC = ByteUtils.readShort(bytes, offset);
        offset += 2;

        // 7. 验证CRC (从userId长度开始到数据结束，跳过CRC字段本身)
        short expectedCRC = ByteUtils.calculateCRC16(bytes, 1, crcOffset, crcOffset + 2, bytes.length);
        if (receivedCRC != expectedCRC) {
            throw new IllegalArgumentException("CRC校验失败, 期望: " + expectedCRC + ", 实际: " + receivedCRC);
        }

        // 8. 用户ID
        String userId = new String(bytes, offset, userIdLen, StandardCharsets.UTF_8);
        offset += userIdLen;

        // 9. 代理ID
        String agentId = new String(bytes, offset, agentIdLen, StandardCharsets.UTF_8);
        offset += agentIdLen;

        // 10. 数据 (Base64编码的字符串)
        byte[] videoDataBytes = Arrays.copyOfRange(bytes, offset, bytes.length);
        String base64Data = new String(videoDataBytes, StandardCharsets.UTF_8);

        // 创建VideoUdpPacket对象
        VideoUdpPacket packet = new VideoUdpPacket();
        packet.setUserId(userId);
        packet.setAgentId(agentId);
        packet.setChunkIndex(chunkIndex);
        packet.setTotalChunks(totalChunks);
        packet.setData(base64Data);

        return packet;
    }
}
