package com.openapi.udp;

import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class UdpTest {

    public static void main(String[] args) {
        System.out.println("=== UDP二进制协议测试 ===");

        // 测试数据
        String userId = "test_user9602534400";
        String agentId = "1984264579602534400";
        int chunkIndex = 3;
        int totalChunks = 4;
        byte[] testData = new byte[500]; // 500字节测试数据
        Arrays.fill(testData, (byte) 0xAA); // 填充 0xAA (10101010)

        System.out.println("原始数据:");
        System.out.println("userId: " + userId);
        System.out.println("agentId: " + agentId);
        System.out.println("chunkIndex: " + chunkIndex);
        System.out.println("totalChunks: " + totalChunks);
        System.out.println("data length: " + testData.length + " bytes");
        System.out.println("data preview: " + bytesToHex(Arrays.copyOf(testData, 16)) + "...");

        // 创建二进制包
        byte[] packet = createBinaryProtocol(userId, agentId, chunkIndex, totalChunks, testData);
        System.out.println("\n创建的数据包:");
        System.out.println("总大小: " + packet.length + " bytes");
        System.out.println("头部预览: " + bytesToHex(Arrays.copyOf(packet, 32)) + "...");

        // 解析二进制包
        BinaryVideoPacket parsedPacket = parseBinaryProtocol(packet);

        System.out.println("\n解析结果:");
        System.out.println("userId: " + parsedPacket.getUserId());
        System.out.println("agentId: " + parsedPacket.getAgentId());
        System.out.println("chunkIndex: " + parsedPacket.getChunkIndex());
        System.out.println("totalChunks: " + parsedPacket.getTotalChunks());
        System.out.println("data length: " + parsedPacket.getData().length + " bytes");
        System.out.println("data preview: " + bytesToHex(Arrays.copyOf(parsedPacket.getData(), 16)) + "...");

        // 验证数据一致性
        boolean success = validatePacket(parsedPacket, userId, agentId, chunkIndex, totalChunks, testData);
        System.out.println("\n数据验证: " + (success ? "✓ 成功" : "✗ 失败"));

        // 测试边界情况
        testEdgeCases();
    }

    /**
     * 创建二进制协议包
     */
    private static byte[] createBinaryProtocol(String userId, String agentId, int chunkIndex, int totalChunks, byte[] data) {
        byte[] userIdBytes = userId.getBytes(StandardCharsets.UTF_8);
        byte[] agentIdBytes = agentId.getBytes(StandardCharsets.UTF_8);

        // 1字节 = 8字符：0~255 所以长度小于255的都可以用1个字节来表示长度
        // 头部固定部分: 4(魔数) + 1(版本) + 1(userId长度) + 1(agentId长度) + 4(chunkIndex) + 4(totalChunks) = 15字节
        int fixedHeaderSize = 15;
        int totalSize = fixedHeaderSize + userIdBytes.length + agentIdBytes.length + data.length;

        byte[] buffer = new byte[totalSize];
        int offset = 0;

        // 1. 魔数 (4字节) - "UDPV"
        System.arraycopy("UDPV".getBytes(StandardCharsets.UTF_8), 0, buffer, offset, 4);
        offset += 4;

        // 2. 版本号 (1字节)
        buffer[offset++] = 1;  // 版本1

        // 3. 用户ID长度 (1字节)
        if (userIdBytes.length > 255) {
            throw new IllegalArgumentException("用户ID过长: " + userIdBytes.length);
        }
        buffer[offset++] = (byte) userIdBytes.length;

        // 4. 代理ID长度 (1字节)
        if (agentIdBytes.length > 255) {
            throw new IllegalArgumentException("代理ID过长: " + agentIdBytes.length);
        }
        buffer[offset++] = (byte) agentIdBytes.length;

        // 5. 分片索引 (4字节)
        writeInt(chunkIndex, buffer, offset);
        offset += 4;

        // 6. 总分片数 (4字节)
        writeInt(totalChunks, buffer, offset);
        offset += 4;

        // 7. 用户ID数据 (变长)
        System.arraycopy(userIdBytes, 0, buffer, offset, userIdBytes.length);
        offset += userIdBytes.length;

        // 8. 代理ID数据 (变长)
        System.arraycopy(agentIdBytes, 0, buffer, offset, agentIdBytes.length);
        offset += agentIdBytes.length;

        // 9. 实际数据 (变长)
        System.arraycopy(data, 0, buffer, offset, data.length);

        return buffer;
    }

    /**
     * 解析二进制协议包
     */
    private static BinaryVideoPacket parseBinaryProtocol(byte[] data) {
        if (data.length < 15) {
            throw new IllegalArgumentException("数据包过短: " + data.length + " bytes");
        }

        int offset = 0;

        // 1. 检查魔数
        byte[] magic = Arrays.copyOfRange(data, offset, offset + 4);
        if (!Arrays.equals(magic, "UDPV".getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("无效的协议魔数: " + new String(magic, StandardCharsets.UTF_8));
        }
        offset += 4;

        // 2. 版本号
        byte version = data[offset++];
        if (version != 1) {
            throw new IllegalArgumentException("不支持的协议版本: " + version);
        }

        // 3. 用户ID长度
        int userIdLen = data[offset++] & 0xFF; // 无符号转换

        // 4. 代理ID长度
        int agentIdLen = data[offset++] & 0xFF; // 无符号转换

        // 5. 分片索引
        int chunkIndex = readInt(data, offset);
        offset += 4;

        // 6. 总分片数
        int totalChunks = readInt(data, offset);
        offset += 4;

        // 7. 用户ID
        String userId = new String(data, offset, userIdLen, StandardCharsets.UTF_8);
        offset += userIdLen;

        // 8. 代理ID
        String agentId = new String(data, offset, agentIdLen, StandardCharsets.UTF_8);
        offset += agentIdLen;

        // 9. 数据
        byte[] videoData = Arrays.copyOfRange(data, offset, data.length);

        return new BinaryVideoPacket(userId, agentId, chunkIndex, totalChunks, videoData);
    }

    /**
     * 写入int到字节数组
     */
    private static void writeInt(int value, byte[] buffer, int offset) {
        buffer[offset] = (byte) (value >> 24);
        buffer[offset + 1] = (byte) (value >> 16);
        buffer[offset + 2] = (byte) (value >> 8);
        buffer[offset + 3] = (byte) value;
    }

    /**
     * 从字节数组读取int
     */
    private static int readInt(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24) |
                ((data[offset + 1] & 0xFF) << 16) |
                ((data[offset + 2] & 0xFF) << 8) |
                (data[offset + 3] & 0xFF);
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    /**
     * 验证数据包
     */
    private static boolean validatePacket(BinaryVideoPacket packet, String expectedUserId, String expectedAgentId,
                                          int expectedChunkIndex, int expectedTotalChunks, byte[] expectedData) {
        return packet.getUserId().equals(expectedUserId) &&
                packet.getAgentId().equals(expectedAgentId) &&
                packet.getChunkIndex() == expectedChunkIndex &&
                packet.getTotalChunks() == expectedTotalChunks &&
                Arrays.equals(packet.getData(), expectedData);
    }

    /**
     * 测试边界情况
     */
    private static void testEdgeCases() {
        System.out.println("\n=== 边界情况测试 ===");

        // 测试1: 最小数据包
        try {
            byte[] minData = new byte[0]; // 空数据
            byte[] packet = createBinaryProtocol("a", "b", 0, 1, minData);
            BinaryVideoPacket parsed = parseBinaryProtocol(packet);
            System.out.println("测试1 - 最小数据包: ✓ 成功, 大小: " + packet.length + " bytes");
        } catch (Exception e) {
            System.out.println("测试1 - 最小数据包: ✗ 失败 - " + e.getMessage());
        }

        // 测试2: 长用户ID
        try {
            String longUserId = "u".repeat(100); // 100字符
            byte[] packet = createBinaryProtocol(longUserId, "agent", 0, 1, new byte[10]);
            System.out.println("测试2 - 长用户ID: ✓ 成功, 用户ID长度: " + longUserId.length());
        } catch (Exception e) {
            System.out.println("测试2 - 长用户ID: ✗ 失败 - " + e.getMessage());
        }

        // 测试3: 最大长度ID (255字符)
        try {
            String maxUserId = "u".repeat(255);
            byte[] packet = createBinaryProtocol(maxUserId, "agent", 0, 1, new byte[10]);
            System.out.println("测试3 - 最大长度ID: ✓ 成功");
        } catch (Exception e) {
            System.out.println("测试3 - 最大长度ID: ✗ 失败 - " + e.getMessage());
        }

        // 测试4: 超长ID (应该失败)
        try {
            String tooLongUserId = "u".repeat(256);
            byte[] packet = createBinaryProtocol(tooLongUserId, "agent", 0, 1, new byte[10]);
            System.out.println("测试4 - 超长ID: ✗ 应该失败但成功了");
        } catch (IllegalArgumentException e) {
            System.out.println("测试4 - 超长ID: ✓ 正确失败 - " + e.getMessage());
        }

        // 测试5: 无效魔数
        try {
            byte[] invalidPacket = new byte[20];
            Arrays.fill(invalidPacket, (byte) 0xFF);
            BinaryVideoPacket parsed = parseBinaryProtocol(invalidPacket);
            System.out.println("测试5 - 无效魔数: ✗ 应该失败但成功了");
        } catch (IllegalArgumentException e) {
            System.out.println("测试5 - 无效魔数: ✓ 正确失败 - " + e.getMessage());
        }

        // 测试6: 数据包过短
        try {
            byte[] shortPacket = new byte[10]; // 小于15字节
            BinaryVideoPacket parsed = parseBinaryProtocol(shortPacket);
            System.out.println("测试6 - 数据包过短: ✗ 应该失败但成功了");
        } catch (IllegalArgumentException e) {
            System.out.println("测试6 - 数据包过短: ✓ 正确失败 - " + e.getMessage());
        }

        // 测试7: 大数据包
        try {
            byte[] largeData = new byte[1200]; // 大数据
            Arrays.fill(largeData, (byte) 0x55);
            byte[] packet = createBinaryProtocol("user", "agent", 999, 1000, largeData);
            System.out.println("测试7 - 大数据包: ✓ 成功, 总大小: " + packet.length + " bytes");
        } catch (Exception e) {
            System.out.println("测试7 - 大数据包: ✗ 失败 - " + e.getMessage());
        }
    }

    /**
     * 二进制视频包数据结构
     */
    @Data
    static class BinaryVideoPacket {
        private final String userId;
        private final String agentId;
        private final int chunkIndex;
        private final int totalChunks;
        private final byte[] data;

        public BinaryVideoPacket(String userId, String agentId, int chunkIndex, int totalChunks, byte[] data) {
            this.userId = userId;
            this.agentId = agentId;
            this.chunkIndex = chunkIndex;
            this.totalChunks = totalChunks;
            this.data = data;
        }

    }
}