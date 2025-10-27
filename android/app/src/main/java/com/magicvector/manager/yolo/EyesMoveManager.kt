package com.magicvector.manager.yolo

import android.os.Handler
import android.os.Looper
import android.view.View
import com.detection.yolov8.targetPoint.TargetPoint
import kotlin.math.abs

object EyesMoveManager {

    // 移动相关参数
    private const val SMOOTH_FACTOR = 0.2f
    private const val MIN_DISTANCE = 2f

    // 复位相关参数
    private const val RESET_DELAY = 2000L // 2秒无调用后开始复位
    private val RESET_TARGET = TargetPoint(0.5f, 0.5f, null, null)

    private var currentLeft = 0f
    private var currentTop = 0f
    private var isInitialized = false
    private var isResetting = false

    // 计时器相关
    private val handler = Handler(Looper.getMainLooper())
    private var resetRunnable: Runnable? = null
    private var lastMoveTime = 0L

    fun moveLayoutToTargetPoint(
        targetPoint: TargetPoint,
        screenWidth: Int,
        screenHeight: Int,
        layout: View,
        isNeedReset: Boolean
    ) {
        // 更新最后调用时间
        updateLastMoveTime()

        // 如果正在复位，取消复位
        if (isResetting) {
            cancelReset()
        }

        layout.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val layoutWidth = layout.measuredWidth
        val layoutHeight = layout.measuredHeight

        // 计算左上角坐标
        var targetLeft = (targetPoint.x * screenWidth) - (layoutWidth / 2)
        var targetTop = (targetPoint.y * screenHeight) - (layoutHeight / 2)

        /*
            边界处理：
            left < 0 -> left = 0; left + layoutWidth > screenWidth -> left = screenWidth - layoutWidth
            top < 0 -> top = 0; top + layoutHeight > screenHeight -> top = screenHeight - layoutHeight
        */
        targetLeft = when {
            targetLeft < 0 -> 0f
            targetLeft + layoutWidth > screenWidth -> (screenWidth - layoutWidth).toFloat()
            else -> targetLeft
        }
        targetTop = when {
            targetTop < 0 -> 0f
            targetTop + layoutHeight > screenHeight -> (screenHeight - layoutHeight).toFloat()
            else -> targetTop
        }

        // 初始化当前位置
        if (!isInitialized) {
            currentLeft = layout.x
            currentTop = layout.y
            isInitialized = true
        }

        // 计算平滑移动
        currentLeft = smoothMove(currentLeft, targetLeft)
        currentTop = smoothMove(currentTop, targetTop)

        // 更新位置
        layout.x = currentLeft
        layout.y = currentTop

        // 根据参数决定是否启动复位计时器
        if (isNeedReset) {
            scheduleReset(screenWidth, screenHeight, layout)
        }
        else {
            // 如果不需要复位，取消可能存在的复位任务
            cancelReset()
        }
    }

    private fun smoothMove(current: Float, target: Float): Float {
        val distance = target - current

        // 如果距离很小，直接到达目标位置避免抖动
        if (abs(distance) < MIN_DISTANCE) {
            return target
        }

        // 弹簧阻尼效果：距离越远移动越快，越近移动越慢
        return current + distance * SMOOTH_FACTOR
    }

    private fun scheduleReset(screenWidth: Int, screenHeight: Int, layout: View) {
        // 取消之前的复位任务
        cancelReset()

        // 创建新的复位任务
        resetRunnable = Runnable {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastMoveTime >= RESET_DELAY) {
                startReset(screenWidth, screenHeight, layout)
            } else {
                // 如果还没到时间，重新调度
                scheduleReset(screenWidth, screenHeight, layout)
            }
        }

        handler.postDelayed(resetRunnable!!, RESET_DELAY)
    }

    private fun startReset(screenWidth: Int, screenHeight: Int, layout: View) {
        isResetting = true

        layout.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val layoutWidth = layout.measuredWidth
        val layoutHeight = layout.measuredHeight

        // 计算复位目标位置 (0.5, 0.5)
        var resetLeft = (RESET_TARGET.x * screenWidth) - (layoutWidth / 2)
        var resetTop = (RESET_TARGET.y * screenHeight) - (layoutHeight / 2)

        // 边界处理
        resetLeft = when {
            resetLeft < 0 -> 0f
            resetLeft + layoutWidth > screenWidth -> (screenWidth - layoutWidth).toFloat()
            else -> resetLeft
        }

        resetTop = when {
            resetTop < 0 -> 0f
            resetTop + layoutHeight > screenHeight -> (screenHeight - layoutHeight).toFloat()
            else -> resetTop
        }

        // 平滑移动到复位位置
        currentLeft = smoothMove(currentLeft, resetLeft)
        currentTop = smoothMove(currentTop, resetTop)

        layout.x = currentLeft
        layout.y = currentTop

        // 检查是否到达复位位置
        val dx = resetLeft - currentLeft
        val dy = resetTop - currentTop
        val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        if (distance > MIN_DISTANCE) {
            // 如果还没到达，继续复位
            handler.postDelayed({
                startReset(screenWidth, screenHeight, layout)
            }, 16) // 约60帧
        } else {
            // 复位完成
            isResetting = false
        }
    }

    private fun cancelReset() {
        resetRunnable?.let {
            handler.removeCallbacks(it)
            resetRunnable = null
        }
        isResetting = false
    }

    // 公共方法：手动触发复位
    fun triggerReset(screenWidth: Int, screenHeight: Int, layout: View) {
        updateLastMoveTime() // 更新时间为现在，避免自动复位干扰
        startReset(screenWidth, screenHeight, layout)
    }

    // 公共方法：强制停止复位
    fun stopReset() {
        cancelReset()
    }

    // 重置状态
    fun reset() {
        cancelReset()
        isInitialized = false
        isResetting = false
        lastMoveTime = 0L
    }

    private fun updateLastMoveTime() {
        lastMoveTime = System.currentTimeMillis()
    }

    // 清理资源
    fun destroy() {
        cancelReset()
        handler.removeCallbacksAndMessages(null)
    }

}