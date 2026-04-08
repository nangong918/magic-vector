package com.magicvector.manager.realtime


import android.graphics.Bitmap
import com.magicvector.manager.vl.UdpVisionManager

/**
 * 视觉管理器
 * 负责 UDP 视频帧发送
 */
class RealtimeChatVisionManager {

    private var udpVisionManager: UdpVisionManager? = null
    private var onVideoFrameCallback: ((Bitmap) -> Unit)? = null

    /**
     * 初始化 UDP 视觉管理器
     */
    fun init(userId: String, agentId: String) {
        udpVisionManager = UdpVisionManager.getInstance().apply {
            initialize(userId, agentId)
        }
    }

    /**
     * 设置视频帧回调（用于 YOLOv8 识别等）
     */
    fun setOnVideoFrameCallback(callback: ((Bitmap) -> Unit)?) {
        this.onVideoFrameCallback = callback
    }

    /**
     * 发送视频帧
     * @param bitmap 视频帧图片
     */
    fun sendVideoFrame(bitmap: Bitmap) {
        // 回调给外部（用于 YOLOv8 识别）
        onVideoFrameCallback?.invoke(bitmap)

        // 通过 UDP 发送视频帧给后端
        udpVisionManager?.sendVideoFrame(bitmap)
    }

    /**
     * 释放资源
     */
    fun release() {
        udpVisionManager?.destroy()
        udpVisionManager = null
        onVideoFrameCallback = null
    }
}