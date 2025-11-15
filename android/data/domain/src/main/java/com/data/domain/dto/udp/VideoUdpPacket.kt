package com.data.domain.dto.udp

import com.core.baseutil.udp.ByteUtils
import com.data.domain.constant.udp.UdpDataType

/**
 * 视频分片数据类（与后端VideoSession对应）
 * 协议：[数据类型:1字节][userId长度字段:1字节][agentId长度字段:1字节][chunkIndex:4字节][totalChunks:4字节][CRC16校验:2字节][userId:20字节][agentId:20字节]
 */
data class VideoUdpPacket(
    val userId: String,
    val agentId: String,
    val chunkIndex: Int,
    val totalChunks: Int,
//    val timestamp: Long = System.currentTimeMillis(),
    val data: String
){
    companion object {
        fun toBytes(packet: VideoUdpPacket): ByteArray {
            val userIdBytes = packet.userId.toByteArray(Charsets.UTF_8)
            val agentIdBytes = packet.agentId.toByteArray(Charsets.UTF_8)
            val dataBytes = packet.data.toByteArray(Charsets.UTF_8)

            // 检查长度限制
            if (userIdBytes.size > 255) {
                throw IllegalArgumentException("用户ID过长: ${userIdBytes.size}")
            }
            if (agentIdBytes.size > 255) {
                throw IllegalArgumentException("代理ID过长: ${agentIdBytes.size}")
            }

            // 计算总大小
            val fixedHeaderSize = 1 + 1 + 1 + 4 + 4 + 2  // 数据类型1 + 长度2 + 索引8 + CRC2 = 13字节
            val totalSize = fixedHeaderSize + userIdBytes.size + agentIdBytes.size + dataBytes.size

            val buffer = ByteArray(totalSize)
            var offset = 0

            // 1. 数据类型 (视频数据)
            buffer[offset++] = UdpDataType.VIDEO.type  // UdpDataType.VIDEO

            // 2. 用户ID长度
            buffer[offset++] = userIdBytes.size.toByte()

            // 3. 代理ID长度
            buffer[offset++] = agentIdBytes.size.toByte()

            // 4. 分片索引
            ByteUtils.writeInt(packet.chunkIndex, buffer, offset)
            offset += 4

            // 5. 总分片数
            ByteUtils.writeInt(packet.totalChunks, buffer, offset)
            offset += 4

            // 6. CRC占位 (先填0，后面计算)
            val crcOffset = offset
            ByteUtils.writeShort(0, buffer, crcOffset)
            offset += 2

            // 7. 用户ID数据
            System.arraycopy(userIdBytes, 0, buffer, offset, userIdBytes.size)
            offset += userIdBytes.size

            // 8. 代理ID数据
            System.arraycopy(agentIdBytes, 0, buffer, offset, agentIdBytes.size)
            offset += agentIdBytes.size

            // 9. 实际数据
            System.arraycopy(dataBytes, 0, buffer, offset, dataBytes.size)

            // 计算CRC (从userId长度开始到数据结束，跳过CRC字段本身)
            val crc = ByteUtils.calculateCRC16(buffer, 1, crcOffset, crcOffset + 2, totalSize)

            // 写入CRC
            ByteUtils.writeShort(crc, buffer, crcOffset)

            return buffer
        }
    }
}
