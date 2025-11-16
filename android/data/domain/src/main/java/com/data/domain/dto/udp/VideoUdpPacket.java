package com.data.domain.dto.udp;

import androidx.annotation.NonNull;

import com.core.baseutil.udp.ByteUtils;
import com.data.domain.constant.BaseConstant;
import com.data.domain.constant.udp.UdpDataType;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class VideoUdpPacket {
    public String userId;
    public String agentId;
    public int chunkIndex;
    public int totalChunks;
    public byte[] data;

    /**
     * 1字节: 数据类型
     * 1字节: userId长度字段
     * 1字节: agentId长度字段
     * 4字节: chunkIndex
     * 4字节: totalChunks
     * 2字节: CRC16校验
     * 1 + 1 + 1 + 4 + 4 + 2 = 13字节
     * 头部预留
     * userId: 20字符，UTF-8编码: 英文数字：1字节/字符 × 20 = 20字节
     * agentId: 20字符，UTF-8编码 英文数字：20字节
     * 固定头部: 13字节 (11 + 2字节CRC)
     * userId数据: 20字节
     * agentId数据: 20字节
     * ---
     * 总计: 53字节
     * [数据类型:1字节][userId长度字段:1字节][agentId长度字段:1字节][chunkIndex:4字节][totalChunks:4字节][CRC16校验:2字节][userId:20字节][agentId:20字节]
     */
    private static final int FIXED_HEADER_SIZE = 13;
    private static final int HEADER_SIZE = FIXED_HEADER_SIZE + 20 + 20;
    public static final int DATA_CHUNK_SIZE = BaseConstant.UDP.MAX_PACKET_SIZE - HEADER_SIZE;
    @NonNull
    public static VideoUdpPacket createVideoPacket(byte[] videoData, int chunkIndex, int totalChunks,
                                                   String userId, String agentId) {
        int start = chunkIndex * DATA_CHUNK_SIZE;
        int end = Math.min(start + DATA_CHUNK_SIZE, videoData.length);
        byte[] chunkData = Arrays.copyOfRange(videoData, start, end);

        VideoUdpPacket packet = new VideoUdpPacket();
        packet.userId = userId;
        packet.agentId = agentId;
        packet.chunkIndex = chunkIndex;
        packet.totalChunks = totalChunks;
        packet.data = chunkData;

        return packet;
    }

    public static byte[] createBinaryProtocolWithCRC(@NonNull VideoUdpPacket packet) {
        byte[] userIdBytes = packet.userId.getBytes(StandardCharsets.UTF_8);
        byte[] agentIdBytes = packet.agentId.getBytes(StandardCharsets.UTF_8);
        byte[] data = packet.data;

        int totalSize = FIXED_HEADER_SIZE + userIdBytes.length + agentIdBytes.length + data.length;

        byte[] buffer = new byte[totalSize];
        int offset = 0;

        buffer[offset++] = UdpDataType.VIDEO.getType();
        buffer[offset++] = (byte) userIdBytes.length;
        buffer[offset++] = (byte) agentIdBytes.length;

        ByteUtils.writeInt(packet.chunkIndex, buffer, offset);
        offset += 4;

        ByteUtils.writeInt(packet.totalChunks, buffer, offset);
        offset += 4;

        int crcOffset = offset;
        ByteUtils.writeShort((short) 0, buffer, crcOffset);
        offset += 2;

        System.arraycopy(userIdBytes, 0, buffer, offset, userIdBytes.length);
        offset += userIdBytes.length;

        System.arraycopy(agentIdBytes, 0, buffer, offset, agentIdBytes.length);
        offset += agentIdBytes.length;

        System.arraycopy(data, 0, buffer, offset, data.length);

        short crc = ByteUtils.calculateCRC16(buffer, 0, crcOffset, crcOffset + 2, totalSize);
        ByteUtils.writeShort(crc, buffer, crcOffset);

        return buffer;
    }


    @NotNull
    public static VideoUdpPacket parseBinaryProtocolWithCRC(byte[] data) {
        if (data.length < FIXED_HEADER_SIZE) {
            throw new IllegalArgumentException("数据包过短: " + data.length + " bytes");
        }

        int offset = 0;

        // 数据类型
        byte udpDataType = data[offset++];

        int userIdLen = data[offset++] & 0xFF;
        int agentIdLen = data[offset++] & 0xFF;

        int chunkIndex = ByteUtils.readInt(data, offset);
        offset += 4;

        int totalChunks = ByteUtils.readInt(data, offset);
        offset += 4;

        int crcOffset = offset;
        short receivedCRC = ByteUtils.readShort(data, crcOffset);
        offset += 2;

        short expectedCRC = ByteUtils.calculateCRC16(data, 0, crcOffset, crcOffset + 2, data.length);

        if (receivedCRC != expectedCRC) {
            throw new IllegalArgumentException("CRC校验失败, 期望: " + expectedCRC + ", 实际: " + receivedCRC);
        }

        String userId = new String(data, offset, userIdLen, StandardCharsets.UTF_8);
        offset += userIdLen;

        String agentId = new String(data, offset, agentIdLen, StandardCharsets.UTF_8);
        offset += agentIdLen;

        byte[] videoData = Arrays.copyOfRange(data, offset, data.length);

        VideoUdpPacket packet = new VideoUdpPacket();
        packet.userId = userId;
        packet.agentId = agentId;
        packet.chunkIndex = chunkIndex;
        packet.totalChunks = totalChunks;
        packet.data = videoData;

        return packet;
    }
}
