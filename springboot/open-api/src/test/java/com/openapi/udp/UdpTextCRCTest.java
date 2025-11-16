package com.openapi.udp;

import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class UdpTextCRCTest {

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
     * 1字节: 版本号
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
     */
    private static final int HEADER_SIZE = 53;
    private static final int DATA_CHUNK_SIZE = MAX_PACKET_SIZE - HEADER_SIZE; // 实际数据分片大小

    public static void main(String[] args) {
        System.out.println("=== UDP文本分片传输测试 (带CRC校验) ===\n");

        // 测试1: 正常传输（不模拟数据损坏）
        System.out.println("🎯 测试1: 正常传输测试");
        testNormalTransmission();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 测试2: 数据损坏传输（模拟数据损坏）
        System.out.println("🎯 测试2: 数据损坏传输测试");
        testCorruptedTransmission();
    }

    /**
     * UDP传输测试主方法
     * @param simulateCorruption 是否模拟数据损坏
     */
    private static void testUdpTransmission(boolean simulateCorruption) {
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
            byte[] packetData = createBinaryProtocolWithCRC(packet);
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

        // 模拟网络传输
        if (simulateCorruption) {
            System.out.println("\n=== 模拟网络传输 (打乱顺序 + 数据损坏测试) ===");
        } else {
            System.out.println("\n=== 模拟网络传输 (只打乱顺序，不模拟数据损坏) ===");
        }

        List<byte[]> receivedPackets = new ArrayList<>(sentPackets);
        Collections.shuffle(receivedPackets);

        // 根据参数决定是否模拟数据损坏
        if (simulateCorruption && !receivedPackets.isEmpty()) {
            byte[] corruptedPacket = receivedPackets.get(0).clone();

            // 更精确的计算数据开始位置
            int headerSize = 1 + 1 + 1 + 4 + 4 + 2; // 版本1 + 长度2 + 索引8 + CRC2 = 16字节
            int userIdLen = 9; // "test_user" 长度
            int agentIdLen = 19; // "1984264579602534400" 长度
            int dataStart = headerSize + userIdLen + agentIdLen;

            if (corruptedPacket.length > dataStart) {
                corruptedPacket[dataStart] ^= (byte) 0xFF; // 翻转数据部分的第一个字节
                receivedPackets.set(0, corruptedPacket);
                System.out.println("已模拟数据损坏: 修改了第一个包的数据部分，位置=" + dataStart);
            }
        }

        System.out.println("接收到的包顺序: ");
        for (int i = 0; i < receivedPackets.size(); i++) {
            try {
                BinaryTextPacket p = parseBinaryProtocolWithCRC(receivedPackets.get(i));
                System.out.printf("包%d: 分片索引=%d, 数据大小=%d, CRC=✅%n",
                        i, p.getChunkIndex(), p.getData().length);
            } catch (IllegalArgumentException e) {
                System.out.printf("包%d: CRC=❌ (%s)%n", i, e.getMessage());
            }
        }

        // 模拟接收端：按索引重组
        System.out.println("\n=== 接收端重组过程 ===");
        Map<Integer, byte[]> receivedChunks = new HashMap<>();
        int corruptedPackets = 0;
        int validPackets = 0;

        for (byte[] packetData : receivedPackets) {
            try {
                BinaryTextPacket packet = parseBinaryProtocolWithCRC(packetData);
                receivedChunks.put(packet.getChunkIndex(), packet.getData());
                validPackets++;
                System.out.printf("接收分片: 索引=%d, 大小=%d bytes, CRC=✅%n",
                        packet.getChunkIndex(), packet.getData().length);
            } catch (IllegalArgumentException e) {
                corruptedPackets++;
                System.out.printf("接收分片: CRC=❌ (%s)%n", e.getMessage());

                // 尝试解析损坏的包来获取索引信息（用于调试）
                try {
                    // 强制解析获取索引信息，即使CRC失败
                    int chunkIndex = extractChunkIndexFromCorruptedPacket(packetData);
                    System.out.printf("  -> 损坏包原本索引: %d%n", chunkIndex);
                } catch (Exception ex) {
                    System.out.printf("  -> 无法识别损坏包的索引%n");
                }
            }
        }

        // 重组数据
        System.out.println("\n=== 数据重组 ===");

        if (corruptedPackets > 0) {
            System.out.println("⚠️ 有 " + corruptedPackets + " 个数据包CRC校验失败");
            System.out.println("✅ 有 " + validPackets + " 个数据包校验成功");

            if (receivedChunks.size() == totalChunks) {
                System.out.println("🎉 幸运！损坏的包不影响完整重组");
                // 继续执行重组
            } else {
                System.out.println("❌ 损坏的包导致无法完整重组");
                System.out.println("缺失的分片索引: " + findMissingChunks(receivedChunks, totalChunks));
                System.out.println("跳过数据验证，因为重组失败");
                return;
            }
        }

        // 执行重组和验证
        try {
            byte[] reassembledData = reassembleData(receivedChunks, totalChunks, textBytes.length);
            String reassembledText = new String(reassembledData, StandardCharsets.UTF_8);

            // 验证结果
            System.out.println("重组后数据长度: " + reassembledData.length + " 字节");
            System.out.println("重组后文本长度: " + reassembledText.length() + " 字符");
            System.out.println("数据完整性验证: " + (Arrays.equals(textBytes, reassembledData) ? "✅ 成功" : "❌ 失败"));
            System.out.println("文本内容验证: " + (text.equals(reassembledText) ? "✅ 成功" : "❌ 失败"));

            // 显示部分重组文本
            System.out.println("\n=== 重组文本预览 ===");
            System.out.println(reassembledText);

        } catch (IllegalStateException e) {
            System.out.println("❌ 重组失败: " + e.getMessage());
        }
    }

    /**
     * 单独测试正常传输（不模拟数据损坏）
     */
    private static void testNormalTransmission() {
        System.out.println("🎯 正常传输测试");
        testUdpTransmission(false);
    }

    /**
     * 单独测试数据损坏传输（模拟数据损坏）
     */
    private static void testCorruptedTransmission() {
        System.out.println("🎯 数据损坏传输测试");
        testUdpTransmission(true);
    }

    /**
     * 从损坏的包中提取分片索引（用于调试）
     */
    private static int extractChunkIndexFromCorruptedPacket(byte[] data) {
        if (data.length < 15) return -1;

        int offset = 4; // 跳过魔数
        offset += 1; // 跳过版本
        offset += 2; // 跳过长度字段

        // 读取分片索引
        return readInt(data, offset);
    }

    /**
     * 查找缺失的分片
     */
    private static List<Integer> findMissingChunks(Map<Integer, byte[]> chunks, int totalChunks) {
        List<Integer> missing = new ArrayList<>();
        for (int i = 0; i < totalChunks; i++) {
            if (!chunks.containsKey(i)) {
                missing.add(i);
            }
        }
        return missing;
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
     * 创建带CRC校验的二进制协议包
     */
    private static byte[] createBinaryProtocolWithCRC(BinaryTextPacket packet) {
        byte[] userIdBytes = packet.getUserId().getBytes(StandardCharsets.UTF_8);
        byte[] agentIdBytes = packet.getAgentId().getBytes(StandardCharsets.UTF_8);
        byte[] data = packet.getData();

        // 头部固定部分 + 2字节CRC
        int fixedHeaderSize = 11 + 2;  // 原15字节 + 2字节CRC
        int totalSize = fixedHeaderSize + userIdBytes.length + agentIdBytes.length + data.length;

        byte[] buffer = new byte[totalSize];
        int offset = 0;

//        // 1. 魔数
//        System.arraycopy("UDPT".getBytes(StandardCharsets.UTF_8), 0, buffer, offset, 4);
//        offset += 4;

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

        // 7. CRC占位 (先填0，后面计算)
        int crcOffset = offset;
        writeShort((short) 0, buffer, crcOffset); // 先填0
        offset += 2;

        // 8. 用户ID数据
        System.arraycopy(userIdBytes, 0, buffer, offset, userIdBytes.length);
        offset += userIdBytes.length;

        // 9. 代理ID数据
        System.arraycopy(agentIdBytes, 0, buffer, offset, agentIdBytes.length);
        offset += agentIdBytes.length;

        // 10. 文本数据
        System.arraycopy(data, 0, buffer, offset, data.length);

        // 计算CRC (从版本号开始到CRC字段之前 + CRC字段之后到数据结束)
        short crc = calculateCRC16(buffer, 4, crcOffset, crcOffset + 2, totalSize);

        // 写入CRC
        writeShort(crc, buffer, crcOffset);

        return buffer;
    }


    /**
     * 解析带CRC校验的二进制协议包
     */
    private static BinaryTextPacket parseBinaryProtocolWithCRC(byte[] data) {
        if (data.length < 13) {
            throw new IllegalArgumentException("数据包过短: " + data.length + " bytes");
        }

        int offset = 0;

//        // 1. 检查魔数
//        byte[] magic = Arrays.copyOfRange(data, offset, offset + 4);
//        if (!Arrays.equals(magic, "UDPT".getBytes(StandardCharsets.UTF_8))) {
//            throw new IllegalArgumentException("无效的协议魔数");
//        }
//        offset += 4;

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

        // 7. 读取CRC值 - 关键修复：在offset当前位置读取
        int crcOffset = offset; // 当前位置就是CRC字段
        short receivedCRC = readShort(data, crcOffset);
        offset += 2; // 跳过CRC字段

        // 8. 计算期望的CRC (跳过CRC字段本身)
        short expectedCRC = calculateCRC16(data, 4, crcOffset, crcOffset + 2, data.length);

        if (receivedCRC != expectedCRC) {
            throw new IllegalArgumentException("CRC校验失败, 期望: " + expectedCRC + ", 实际: " + receivedCRC);
        }

        // 9. 用户ID
        String userId = new String(data, offset, userIdLen, StandardCharsets.UTF_8);
        offset += userIdLen;

        // 10. 代理ID
        String agentId = new String(data, offset, agentIdLen, StandardCharsets.UTF_8);
        offset += agentIdLen;

        // 11. 文本数据
        byte[] textData = Arrays.copyOfRange(data, offset, data.length);

        return new BinaryTextPacket(userId, agentId, chunkIndex, totalChunks, textData);
    }

    /**
     * 计算CRC16校验和
     */
    private static short calculateCRC16(byte[] data, int start, int skipStart, int skipEnd, int end) {
        int crc = 0xFFFF;

        // 计算第一部分: start 到 skipStart
        for (int i = start; i < skipStart; i++) {
            crc = updateCRC16(crc, data[i]);
        }

        // 跳过 skipStart 到 skipEnd 的范围

        // 计算第二部分: skipEnd 到 end
        for (int i = skipEnd; i < end; i++) {
            crc = updateCRC16(crc, data[i]);
        }

        return (short) crc;
    }

    /**
     * 更新CRC16计算
     */
    private static int updateCRC16(int crc, byte b) {
        crc ^= (b & 0xFF);
        for (int j = 0; j < 8; j++) {
            if ((crc & 0x0001) != 0) {
                crc >>= 1;
                crc ^= 0xA001;
            } else {
                crc >>= 1;
            }
        }
        return crc;
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
            byte[] packetData = createBinaryProtocolWithCRC(packet);
            BinaryTextPacket parsed = parseBinaryProtocolWithCRC(packetData);
            System.out.println("测试1 - 空文本: ✅ 成功");
        } catch (Exception e) {
            System.out.println("测试1 - 空文本: ❌ 失败 - " + e.getMessage());
        }

        // 测试2: 单包刚好等于MTU
        try {
            byte[] exactSizeData = new byte[DATA_CHUNK_SIZE];
            Arrays.fill(exactSizeData, (byte) 'A');
            BinaryTextPacket packet = new BinaryTextPacket("u", "a", 0, 1, exactSizeData);
            byte[] packetData = createBinaryProtocolWithCRC(packet);
            System.out.println("测试2 - 刚好MTU: ✅ 成功, 大小: " + packetData.length + " bytes");
        } catch (Exception e) {
            System.out.println("测试2 - 刚好MTU: ❌ 失败 - " + e.getMessage());
        }

        // 测试3: CRC校验失败
        try {
            byte[] testData = "测试数据".getBytes(StandardCharsets.UTF_8);
            BinaryTextPacket packet = new BinaryTextPacket("user", "agent", 0, 1, testData);
            byte[] packetData = createBinaryProtocolWithCRC(packet);

            // 故意损坏数据
            packetData[packetData.length - 1] ^= 0x0F; // 修改数据

            BinaryTextPacket parsed = parseBinaryProtocolWithCRC(packetData);
            System.out.println("测试3 - CRC校验失败: ❌ 应该失败但成功了");
        } catch (IllegalArgumentException e) {
            System.out.println("测试3 - CRC校验失败: ✅ 正确失败 - " + e.getMessage());
        }

        // 测试4: 缺失分片
        try {
            Map<Integer, byte[]> incompleteChunks = new HashMap<>();
            incompleteChunks.put(0, "第一部分".getBytes());
            incompleteChunks.put(2, "第三部分".getBytes()); // 缺少索引1
            byte[] result = reassembleData(incompleteChunks, 3, 100);
            System.out.println("测试4 - 缺失分片: ❌ 应该失败但成功了");
        } catch (IllegalStateException e) {
            System.out.println("测试4 - 缺失分片: ✅ 正确失败 - " + e.getMessage());
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

    private static void writeShort(short value, byte[] buffer, int offset) {
        buffer[offset] = (byte) (value >> 8);
        buffer[offset + 1] = (byte) value;
    }

    private static short readShort(byte[] data, int offset) {
        return (short) (((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF));
    }

    /**
     * 二进制文本包数据结构
     */
    @Getter
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