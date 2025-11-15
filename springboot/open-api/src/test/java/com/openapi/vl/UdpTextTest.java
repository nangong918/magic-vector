package com.openapi.vl;

import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class UdpTextTest {

    private static final String text = """
            什么是 闪猫侠 AI 桌面人形机器人？
            本机器人灵感来源于由 Camilo Parra Palacio 于 2016 年创建的 Otto DIY 开源项目，闪猫侠研发团队通过自研端侧算法、外观设计、机械结构等，结合 3D 打印与 ‮ 用通 ‬ 电子元件创造万物的理念，让小白用户也可以超低成本仅需 30 分钟即可手搓一 ‮ 专个 ‬ 属桌面 AI 机器人！通过云端一体的架构给予了桌面机器人 AI 语音对话、AI 动作反馈、AI 视觉系统等能力，产品深 ‮ 融度 ‬ 合了 ‮ 感情 ‬ 陪伴、创客教育、3D 打印、AI 大模型、具身智能、智能家 ‮ 等居 ‬ 多元场景，并支持一键 ‮ 入接 ‬ 闪猫侠 AI、小智 AI、涂鸦智能、火山引擎等主流 AI 服务，是一个 AI 桌面机器人的智能生态硬件平台。我们的目标是大幅降低 AI 机器人制作的门槛,让 AI 和机器人真正的进入物理世界，走进千家万户。
            
            小智 AI 已实现功能
            多种联网方式：支持 Wi-Fi 和 ML307 Cat.1 4G 连接
            智能交互：语音、BOOT 键唤醒和打断，支持点击和长按两种触发方式
            离线语音唤醒：使用 ESP-SR 技术，无需联网即可唤醒
            流式语音对话：支持 WebSocket 或 UDP 协议的实时对话
            多语言识别：支持国语、粤语、英语、日语、韩语五种语言（SenseVoice）
            声纹识别：可识别是谁在呼叫 AI（3D Speaker 技术）
            高质量语音合成：集成火山引擎或 CosyVoice 的大模型 TTS
            AI 大脑：接入 Qwen、DeepSeek、Doubao 等大型语言模型
            个性化定制：可配置的提示词和音色，创建自定义角色
            记忆功能：具备短期记忆，每轮对话后自我总结
            视觉显示：支持 OLED/LCD 显示屏，可显示信号强弱或对话内容
            表情系统：LCD 能显示丰富表情图片
            多语言界面：支持中文、英文等多种语言
            涂鸦智能 AI 已实现功能
            智能家居控制：支持 涂鸦全生态的智能家居语音控制
            多种联网方式：支持 Wi-Fi 和 ML307 Cat.1 4G 连接
            智能交互：语音、BOOT 键唤醒和打断，支持点击和长按两种触发方式
            离线语音唤醒：使用 ESP-SR 技术，无需联网即可唤醒
            流式语音对话：支持 WebSocket 或 UDP 协议的实时对话
            多语言识别：支持国语、粤语、英语、日语、韩语五种语言（SenseVoice）
            声纹识别：可识别是谁在呼叫 AI（3D Speaker 技术）
            高质量语音合成：集成火山引擎或 CosyVoice 的大模型 TTS
            AI 大脑：接入 Qwen、DeepSeek、Doubao 等大型语言模型
            个性化定制：可配置的提示词和音色，创建自定义角色
            记忆功能：具备短期记忆，每轮对话后自我总结
            视觉显示：支持 OLED/LCD 显示屏，可显示信号强弱或对话内容
            表情系统：LCD 能显示丰富表情图片
            多语言界面：支持中文、英文等多种语言
            闪猫侠 AI 服务已实现功能（内测）
            多种联网方式：支持 Wi-Fi 和 ML307 Cat.1 4G 连接
            智能交互：语音、BOOT 键唤醒和打断，支持点击和长按两种触发方式
            离线语音唤醒：使用 ESP-SR 技术，无需联网即可唤醒
            流式语音对话：支持 WebSocket 或 UDP 协议的实时对话
            多语言识别：支持国语、粤语、英语、日语、韩语、俄语、西班牙语、阿拉伯语、越南语、意大利语、乌克兰语等多种语言
            声纹识别：可识别是谁在呼叫 AI（3D Speaker 技术）
            高质量语音合成：集成火山引擎、 CosyVoice 的大模型等的 TTS
            AI 大脑：接入闪猫侠 SuperCat LLM2、Qwen、DeepSeek、Doubao 、文心一言等大型语言模型
            个性化定制：可配置的提示词和音色，创建自定义角色
            记忆功能：具备短期记忆和长期记忆（可选），每轮对话后自我总结
            视觉显示：支持 OLED/LCD 显示屏，可显示信号强弱或对话内容
            表情系统：LCD 能显示丰富表情图片
            多语言界面：支持中文、英文等多种语言
            """;

    private static final int MAX_PACKET_SIZE = 1450; // MTU安全值
    /**
     * 4字节: 魔数 "UDPV"
     * 1字节: 版本号
     * 1字节: userId长度字段
     * 1字节: agentId长度字段
     * 4字节: chunkIndex
     * 4字节: totalChunks
     * 头部预留
     * userId: 20字符，UTF-8编码: 英文数字：1字节/字符 × 20 = 20字节
     * agentId: 20字符，UTF-8编码 英文数字：20字节
     * 固定头部: 15字节
     * userId数据: 20字节
     * agentId数据: 20字节
     * ---
     * 总计: 55字节
     */
    private static final int HEADER_SIZE = 55;
    private static final int DATA_CHUNK_SIZE = MAX_PACKET_SIZE - HEADER_SIZE; // 实际数据分片大小

    public static void main(String[] args) {
        System.out.println("=== UDP文本分片传输测试 ===\n");

        // 原始文本信息
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        System.out.println("原始文本长度: " + text.length() + " 字符");
        System.out.println("原始数据长度: " + textBytes.length + " 字节");
        System.out.println("分片大小: " + DATA_CHUNK_SIZE + " 字节");

        int totalChunks = (int) Math.ceil((double) textBytes.length / DATA_CHUNK_SIZE);
        System.out.println("需要分片数: " + totalChunks);
        System.out.println();

        // 模拟发送端：分片发送
        System.out.println("=== 发送端分片过程 ===");
        List<byte[]> sentPackets = new ArrayList<>();
        Map<Integer, BinaryTextPacket> sentPacketsMap = new HashMap<>();

        String userId = "test_user";
        String agentId = "1984264579602534400";

        for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
            BinaryTextPacket packet = createBinaryPacket(textBytes, chunkIndex, totalChunks, userId, agentId);
            byte[] packetData = createBinaryProtocol(packet);
            sentPackets.add(packetData);
            sentPacketsMap.put(chunkIndex, packet);

            System.out.printf("分片 %d/%d: 数据大小=%d, 总包大小=%d bytes%n",
                    chunkIndex + 1, totalChunks,
                    packet.getData().length, packetData.length);

            // 验证包大小
            if (packetData.length > MAX_PACKET_SIZE) {
                System.out.println("❌ 错误: 数据包超过MTU限制!");
            }
        }

        // 模拟网络传输：打乱包顺序
        System.out.println("\n=== 模拟网络传输 (打乱顺序) ===");
        List<byte[]> receivedPackets = new ArrayList<>(sentPackets);
        Collections.shuffle(receivedPackets);

        System.out.println("接收到的包顺序: ");
        for (int i = 0; i < receivedPackets.size(); i++) {
            BinaryTextPacket p = parseBinaryProtocol(receivedPackets.get(i));
            System.out.printf("包%d: 分片索引=%d, 数据大小=%d%n",
                    i, p.getChunkIndex(), p.getData().length);
        }

        // 模拟接收端：按索引重组
        System.out.println("\n=== 接收端重组过程 ===");
        Map<Integer, byte[]> receivedChunks = new HashMap<>();

        for (byte[] packetData : receivedPackets) {
            BinaryTextPacket packet = parseBinaryProtocol(packetData);
            receivedChunks.put(packet.getChunkIndex(), packet.getData());
            System.out.printf("接收分片: 索引=%d, 大小=%d bytes%n",
                    packet.getChunkIndex(), packet.getData().length);
        }

        // 重组数据
        System.out.println("\n=== 数据重组 ===");
        byte[] reassembledData = reassembleData(receivedChunks, totalChunks, textBytes.length);
        String reassembledText = new String(reassembledData, StandardCharsets.UTF_8);

        // 验证结果
        System.out.println("重组后数据长度: " + reassembledData.length + " 字节");
        System.out.println("重组后文本长度: " + reassembledText.length() + " 字符");
        System.out.println("数据完整性验证: " + (Arrays.equals(textBytes, reassembledData) ? "✅ 成功" : "❌ 失败"));
        System.out.println("文本内容验证: " + (text.equals(reassembledText) ? "✅ 成功" : "❌ 失败"));

        // 显示部分重组文本
        System.out.println("\n=== 重组文本预览 ===");
//        System.out.println(reassembledText.substring(0, Math.min(100, reassembledText.length())) + "...");
        System.out.println(reassembledText);

        // 测试边界情况
        testEdgeCases();
    }

    /**
     * 创建二进制文本包
     */
    private static BinaryTextPacket createBinaryPacket(byte[] textData, int chunkIndex, int totalChunks,
                                                       String userId, String agentId) {
        int start = chunkIndex * DATA_CHUNK_SIZE;
        int end = Math.min(start + DATA_CHUNK_SIZE, textData.length);
        byte[] chunkData = Arrays.copyOfRange(textData, start, end);

        return new BinaryTextPacket(userId, agentId, chunkIndex, totalChunks, chunkData);
    }

    /**
     * 创建二进制协议包
     */
    private static byte[] createBinaryProtocol(BinaryTextPacket packet) {
        byte[] userIdBytes = packet.getUserId().getBytes(StandardCharsets.UTF_8);
        byte[] agentIdBytes = packet.getAgentId().getBytes(StandardCharsets.UTF_8);
        byte[] data = packet.getData();

        // 头部固定部分
        int fixedHeaderSize = 15;
        int totalSize = fixedHeaderSize + userIdBytes.length + agentIdBytes.length + data.length;

        byte[] buffer = new byte[totalSize];
        int offset = 0;

        // 1. 魔数
        System.arraycopy("UDPT".getBytes(StandardCharsets.UTF_8), 0, buffer, offset, 4);
        offset += 4;

        // 2. 版本号
        buffer[offset++] = 1;

        // 3. 用户ID长度
        buffer[offset++] = (byte) userIdBytes.length;

        // 4. 代理ID长度
        buffer[offset++] = (byte) agentIdBytes.length;

        // 5. 分片索引
        writeInt(packet.getChunkIndex(), buffer, offset);
        offset += 4;

        // 6. 总分片数
        writeInt(packet.getTotalChunks(), buffer, offset);
        offset += 4;

        // 7. 用户ID数据
        System.arraycopy(userIdBytes, 0, buffer, offset, userIdBytes.length);
        offset += userIdBytes.length;

        // 8. 代理ID数据
        System.arraycopy(agentIdBytes, 0, buffer, offset, agentIdBytes.length);
        offset += agentIdBytes.length;

        // 9. 文本数据
        System.arraycopy(data, 0, buffer, offset, data.length);

        return buffer;
    }

    /**
     * 解析二进制协议包
     */
    private static BinaryTextPacket parseBinaryProtocol(byte[] data) {
        if (data.length < 15) {
            throw new IllegalArgumentException("数据包过短: " + data.length + " bytes");
        }

        int offset = 0;

        // 1. 检查魔数
        byte[] magic = Arrays.copyOfRange(data, offset, offset + 4);
        if (!Arrays.equals(magic, "UDPT".getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("无效的协议魔数");
        }
        offset += 4;

        // 2. 版本号
        byte version = data[offset++];
        if (version != 1) {
            throw new IllegalArgumentException("不支持的协议版本: " + version);
        }

        // 3. 用户ID长度
        int userIdLen = data[offset++] & 0xFF;

        // 4. 代理ID长度
        int agentIdLen = data[offset++] & 0xFF;

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

        // 9. 文本数据
        byte[] textData = Arrays.copyOfRange(data, offset, data.length);

        return new BinaryTextPacket(userId, agentId, chunkIndex, totalChunks, textData);
    }

    /**
     * 重组数据
     */
    private static byte[] reassembleData(Map<Integer, byte[]> chunks, int totalChunks, int totalLength) {
        byte[] result = new byte[totalLength];
        int currentPos = 0;

        // 按索引顺序重组
        for (int i = 0; i < totalChunks; i++) {
            byte[] chunk = chunks.get(i);
            if (chunk == null) {
                throw new IllegalStateException("缺少分片: " + i);
            }
            System.arraycopy(chunk, 0, result, currentPos, chunk.length);
            currentPos += chunk.length;
        }

        return result;
    }

    /**
     * 测试边界情况
     */
    private static void testEdgeCases() {
        System.out.println("\n=== 边界情况测试 ===");

        // 测试1: 空文本
        try {
            byte[] emptyData = new byte[0];
            BinaryTextPacket packet = new BinaryTextPacket("user", "agent", 0, 1, emptyData);
            byte[] packetData = createBinaryProtocol(packet);
            BinaryTextPacket parsed = parseBinaryProtocol(packetData);
            System.out.println("测试1 - 空文本: ✅ 成功");
        } catch (Exception e) {
            System.out.println("测试1 - 空文本: ❌ 失败 - " + e.getMessage());
        }

        // 测试2: 单包刚好等于MTU
        try {
            byte[] exactSizeData = new byte[DATA_CHUNK_SIZE];
            Arrays.fill(exactSizeData, (byte) 'A');
            BinaryTextPacket packet = new BinaryTextPacket("u", "a", 0, 1, exactSizeData);
            byte[] packetData = createBinaryProtocol(packet);
            System.out.println("测试2 - 刚好MTU: ✅ 成功, 大小: " + packetData.length + " bytes");
        } catch (Exception e) {
            System.out.println("测试2 - 刚好MTU: ❌ 失败 - " + e.getMessage());
        }

        // 测试3: 缺失分片
        try {
            Map<Integer, byte[]> incompleteChunks = new HashMap<>();
            incompleteChunks.put(0, "第一部分".getBytes());
            incompleteChunks.put(2, "第三部分".getBytes()); // 缺少索引1
            byte[] result = reassembleData(incompleteChunks, 3, 100);
            System.out.println("测试3 - 缺失分片: ❌ 应该失败但成功了");
        } catch (IllegalStateException e) {
            System.out.println("测试3 - 缺失分片: ✅ 正确失败 - " + e.getMessage());
        }
    }

    // 工具方法
    private static void writeInt(int value, byte[] buffer, int offset) {
        buffer[offset] = (byte) (value >> 24);
        buffer[offset + 1] = (byte) (value >> 16);
        buffer[offset + 2] = (byte) (value >> 8);
        buffer[offset + 3] = (byte) value;
    }

    private static int readInt(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24) |
                ((data[offset + 1] & 0xFF) << 16) |
                ((data[offset + 2] & 0xFF) << 8) |
                (data[offset + 3] & 0xFF);
    }

    /**
     * 二进制文本包数据结构
     */
    @Data
    static class BinaryTextPacket {
        private final String userId;
        private final String agentId;
        private final int chunkIndex;
        private final int totalChunks;
        private final byte[] data;

        public BinaryTextPacket(String userId, String agentId, int chunkIndex, int totalChunks, byte[] data) {
            this.userId = userId;
            this.agentId = agentId;
            this.chunkIndex = chunkIndex;
            this.totalChunks = totalChunks;
            this.data = data;
        }

    }
}