package com.magicvector.manager.vl

import android.graphics.Bitmap
import android.util.Base64
import com.core.baseutil.log.Log
import com.data.domain.constant.BaseConstant
import com.data.domain.dto.udp.VideoUdpPacket
import com.magicvector.MainApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.math.ceil

class UdpVisionManager {

    companion object {
        @Volatile
        private var instance: UdpVisionManager? = null

        fun getInstance(): UdpVisionManager {
            return instance ?: synchronized(this) {
                instance ?: UdpVisionManager().also { instance = it }
            }
        }
    }

    // 配置参数
    private val serverIp = BaseConstant.ConstantUrl.TEST_HOST // 替换为实际服务器IP
    private val serverPort = BaseConstant.UDP.PORT
    private val chunkSize = BaseConstant.UDP.CHUNK_SIZE // 4KB分片大小
    private val maxPacketSize = BaseConstant.UDP.MAX_PACKET_SIZE // UDP包最大64KB

    // 会话管理
//    private var currentSessionId: String? = null
    private var currentUserId: String = ""
    private var currentAgentId: String = ""

    // UDP相关
    private var datagramSocket: DatagramSocket? = null
    private val gson = MainApplication.GSON
    var isInitialized = false

    // 帧率控制
    private var lastSendTime = 0L
    private val minFrameInterval = BaseConstant.UDP.MIN_FRAME_INTERVAL // 最小发送间隔(ms)，10fps

    /**
     * 初始化UDP管理器
     */
    fun initialize(userId: String, agentId: String) {
        this.currentUserId = userId
        this.currentAgentId = agentId
//        this.currentSessionId = generateSessionId()

        try {
            datagramSocket = DatagramSocket().apply {
                soTimeout = BaseConstant.UDP.UDP_TIMEOUT // 5秒超时
                broadcast = false
            }
            Log.d("UdpVisionManager", "UDP管理器初始化成功")
            isInitialized = true
        } catch (e: Exception) {
            Log.e("UdpVisionManager", "UDP初始化失败", e)
        }
    }

    fun destroy() {
        datagramSocket?.close()
        isInitialized = false
        currentUserId = ""
        currentAgentId = ""
    }

    /**
     * 处理视频帧并发送
     */
    fun sendVideoFrame(bitmap: Bitmap) {
        if (!isInitialized) {
            Log.w("UdpVisionManager", "UDP管理器未初始化")
            return
        }
        // 帧率控制
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSendTime < minFrameInterval) {
            return
        }
        lastSendTime = currentTime

        // 在后台线程处理
        CoroutineScope(Dispatchers.IO).launch {
            try {
                processAndSendFrame(bitmap)
            } catch (e: Exception) {
                Log.e("UdpVisionManager", "发送视频帧失败", e)
            }
        }
    }

    /**
     * 处理并发送视频帧
     */
    private fun processAndSendFrame(bitmap: Bitmap) {
        // 1. 将Bitmap转换为JPEG byte数组
        val jpegData = bitmapToJpeg(bitmap, quality = BaseConstant.UDP.BITMAP_QUALITY) // 70%质量

        // 2. 分片处理
        val totalChunks = ceil(jpegData.size.toDouble() / chunkSize).toInt()

        // 3. 发送每个分片
        for (chunkIndex in 0 until totalChunks) {
            val chunkData = createUdpPacket(jpegData, chunkIndex, totalChunks)
            sendUdpChunk(chunkData)

            // 小延迟避免网络拥塞
            if (chunkIndex % 5 == 0) {
                Thread.sleep(1)
            }
        }

        Log.d("UdpVisionManager", "视频帧发送完成: ${jpegData.size} bytes, $totalChunks chunks")
    }

    /**
     * 将Bitmap转换为JPEG字节数组
     */
    private fun bitmapToJpeg(bitmap: Bitmap, quality: Int): ByteArray {
        return ByteArrayOutputStream().use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.toByteArray()
        }
    }

    /**
     * 获取分片数据
     */
    private fun createUdpPacket(
        videoData: ByteArray,
        chunkIndex: Int,
        totalChunks: Int
    ): VideoUdpPacket {
        val start = chunkIndex * chunkSize
        val end = minOf(start + chunkSize, videoData.size)
        val chunkBytes = videoData.copyOfRange(start, end)

        // Base64编码
        val base64Data = Base64.encodeToString(chunkBytes, Base64.NO_WRAP)

        return VideoUdpPacket(
            userId = currentUserId,
            agentId = currentAgentId,
//            sessionId = currentSessionId ?: generateSessionId(),
            chunkIndex = chunkIndex,
            totalChunks = totalChunks,
//            timestamp = System.currentTimeMillis(),
            data = base64Data
        )
    }

    private fun sendUdpChunk(udpPacket: VideoUdpPacket) {
        try {
            val packetData = VideoUdpPacket.toBytes(udpPacket)

            // 检查包大小
            if (packetData.size > maxPacketSize) {
                Log.w("UdpVisionManager", "UDP包过大: ${packetData.size} bytes")
                return
            }

            val serverAddress = InetAddress.getByName(serverIp)
            val packet = DatagramPacket(
                packetData,
                packetData.size,
                serverAddress,
                serverPort
            )

            datagramSocket?.send(packet)

        } catch (e: Exception) {
            Log.e("UdpVisionManager", "发送UDP分片失败: ${e.message}")
        }
    }

    private fun sendUdpChunkJSON(udpPacket: VideoUdpPacket) {
        try {
            val jsonString = gson.toJson(udpPacket)
            Log.i("UdpVisionManager", "发送UDP分片: $jsonString")
            val packetData = jsonString.toByteArray(Charsets.UTF_8)

            // 检查包大小
            if (packetData.size > maxPacketSize) {
                Log.w("UdpVisionManager", "UDP包过大: ${packetData.size} bytes")
                return
            }

            val serverAddress = InetAddress.getByName(serverIp)
            val packet = DatagramPacket(
                packetData,
                packetData.size,
                serverAddress,
                serverPort
            )

            datagramSocket?.send(packet)

        } catch (e: Exception) {
            Log.e("UdpVisionManager", "发送UDP分片失败: ${e.message}")
        }
    }
}