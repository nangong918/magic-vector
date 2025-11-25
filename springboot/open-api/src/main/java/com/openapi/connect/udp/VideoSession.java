package com.openapi.connect.udp;

import lombok.Data;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 13225
 * @date 2025/11/13 12:56
 * 取消String[] chunks;
 * 因为需要将全部的bytes数据进行拼接之后再转为Base64，而不能直接转为Base64在StringBuffer拼接
 * 因为：
 * 1. Base64编码会改变数据结构和边界
 *    - Base64编码将3个字节转换为4个字符，每个分片单独编码后边界不匹配
 *    - 分片级别的Base64拼接不等于完整数据的Base64编码
 *    - 会导致合并后的Base64字符串无法正确解码
 * <p>
 * 2. 数据完整性无法保证
 *    - 每个分片的Base64字符串可能包含填充字符(=)
 *    - 拼接时填充字符位置错误，破坏数据完整性
 *    - 最终合并的Base64字符串格式错误，API无法解析
 * <p>
 * 3. 性能考虑
 *    - 每个分片单独Base64编码增加CPU开销
 *    - 字符串拼接比字节数组合并效率低
 *    - 一次性编码完整数据更高效
 * <p>
 * 正确做法：
 * - 存储原始字节数据(Map<Integer, byte[]> chunkMap)
 * - 合并所有分片的字节数据
 * - 对完整字节数据一次性进行Base64编码
 * - 确保编码后的数据格式正确，可被API正常解析
 * <p>
 * 示例：
 * 错误：分片1(Base64) + 分片2(Base64) ≠ 完整(Base64)
 * 正确：分片1(bytes) + 分片2(bytes) → 完整(bytes) → Base64
 */
@Data
public class VideoSession {
    private final String userId;
    private final String agentId;
    private final int totalChunks;
    private final Map<Integer, byte[]> chunkMap;
//    private final String[] chunks;
    private final AtomicInteger receivedCount = new AtomicInteger(0);

    public VideoSession(String userId, String agentId, int totalChunks) {
        this.userId = userId;
        this.agentId = agentId;
        this.totalChunks = totalChunks;
//        this.chunks = new String[totalChunks];
        chunkMap = new HashMap<>(totalChunks);
    }

    public synchronized void addChunk(int index, byte[] data) {
//        if (index >= 0 && index < totalChunks && chunks[index] == null) {
//            // byte[] -> Base64Str
//            String dataBase64 = Base64.getEncoder().encodeToString(data);
//            // 数据重排序
//            chunks[index] = dataBase64;
//            receivedCount.incrementAndGet();
//        }
        if (index >= 0 && index < totalChunks && chunkMap.get(index) == null) {
            chunkMap.put(index, data);
            receivedCount.incrementAndGet();
        }
    }

    public boolean isComplete() {
        return receivedCount.get() == totalChunks;
    }

    // 当前端传递的数据的Video的时候
    public String getCompleteVideoData() {
        if (!isComplete()) {
            throw new IllegalStateException("Session not complete");
        }

        try {
            // 1. 合并所有分片的字节数据
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            for (int i = 0; i < totalChunks; i++) {
                byte[] chunk = chunkMap.get(i);
                if (chunk != null) {
                    outputStream.write(chunk);
                } else {
                    throw new RuntimeException("分片 " + i + " 缺失");
                }
            }

            // 2. 将完整的字节数据转换为base64
            byte[] completeData = outputStream.toByteArray();

            return Base64.getEncoder().encodeToString(completeData);

        } catch (Exception e) {
            throw new RuntimeException("合并视频数据失败: " + e.getMessage(), e);
        }
    }

    // 当前端传递的数据的Image的时候
    public String getCompleteImageData() {
        if (!isComplete()) {
            throw new IllegalStateException("Session not complete");
        }

        try {
            // 1. 合并所有分片的字节数据
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            for (int i = 0; i < totalChunks; i++) {
                byte[] chunk = chunkMap.get(i);
                if (chunk != null) {
                    outputStream.write(chunk);
                } else {
                    throw new RuntimeException("分片 " + i + " 缺失");
                }
            }

            // 2. 将完整的字节数据转换为base64
            byte[] completeData = outputStream.toByteArray();

            return Base64.getEncoder().encodeToString(completeData);

        } catch (Exception e) {
            throw new RuntimeException("合并图片数据失败: " + e.getMessage(), e);
        }
    }
}
