package com.data.domain.dto.udp

/**
 * 视频分片数据类（与后端VideoSession对应）
 */
data class VideoUdpPacket(
    val userId: String,
    val agentId: String,
    val chunkIndex: Int,
    val totalChunks: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val data: String
)
