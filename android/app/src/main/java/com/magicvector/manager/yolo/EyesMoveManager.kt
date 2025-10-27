package com.magicvector.manager.yolo

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import com.detection.yolov8.targetPoint.TargetPoint
import kotlinx.coroutines.Runnable
import kotlin.math.abs

object EyesMoveManager {

    const val TAG = "EyesMoveManager"
    // 移动相关参数
    private const val SMOOTH_FACTOR = 0.2f
    private const val MIN_DISTANCE = 2f
    private const val FPS = 30
    private const val FRAME_DELAY = 1000L / FPS // 每帧延迟时间
    private var moveRunnable: Runnable? = null

    // 复位相关参数
    private const val RESET_DELAY = 2000L // 2秒无调用后开始复位
    private val RESET_TARGET = TargetPoint(0.5f, 0.5f, null, null)
    private var currentTargetPoint: TargetPoint = RESET_TARGET

    private var isInitialized = false
    private var isResetting = false
    private var isMoving = false

    // 计时器相关
    private val handler = Handler(Looper.getMainLooper())
    private var resetRunnable: Runnable? = null
    private var lastMoveTime = 0L

    fun moveLayoutToTargetPoint(
        targetPoint: TargetPoint,
        screenWidth: Int,
        screenHeight: Int,
        layout: View,
        isThisReset: Boolean
    ) {
        // 正在移动，返回任务
        if (isMoving){
            updateLastMoveTime()
            return
        }

        // 更新最后调用时间
        updateLastMoveTime()
        // 取消复位
        cancelReset()

        // 记录targetPoint
        currentTargetPoint = targetPoint

        // 启动移动循环
        startMoveLoop(screenWidth, screenHeight, layout, isThisReset)
    }

    private fun moveToTargetPoint(
        targetPoint: TargetPoint,
        screenWidth: Int,
        screenHeight: Int,
        layout: View,
        isThisReset: Boolean
    ) {
        layout.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val layoutWidth = layout.width
        val layoutHeight = layout.height

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

        // 计算平滑移动
        layout.x = smoothMove(layout.x, targetLeft)
        layout.y = smoothMove(layout.y, targetTop)

        // 检查是否移动结束
        val isFinishMove = abs(layout.x - targetLeft) < MIN_DISTANCE && abs(layout.y - targetTop) < MIN_DISTANCE
        if (isFinishMove) {
            isMoving = false

            // 当前是否是reset操作
            if (isThisReset){
                Log.d(TAG, "当前就是reset操作，不进行复位")
                return
            }

            resetRunnable = Runnable {
                // 移动结束，开始复位
                moveLayoutToTargetPoint(
                    RESET_TARGET,
                    screenWidth,
                    screenHeight,
                    layout,
                    true
                )
            }
            // 开始调用延迟检查
            handler.postDelayed(
                resetRunnable!!,
                RESET_DELAY
            )
        }
    }

    private fun startMoveLoop(
        screenWidth: Int,
        screenHeight: Int,
        layout: View,
        isThisReset: Boolean
    ) {
        if (isMoving) return

        isMoving = true
        moveRunnable = object : Runnable {
            override fun run() {
                if (!isMoving) return

                moveToTargetPoint(
                    currentTargetPoint,
                    screenWidth,
                    screenHeight,
                    layout,
                    isThisReset
                )

                // 如果移动未完成，继续下一帧
                if (isMoving) {
                    handler.postDelayed(this, FRAME_DELAY)
                }
            }
        }

        // 立即执行第一帧
        handler.post(moveRunnable!!)
    }

    private fun stopMoveLoop() {
        isMoving = false
        moveRunnable?.let {
            handler.removeCallbacks(it)
            moveRunnable = null
        }
    }

    private fun cancelReset() {
        resetRunnable?.let {
            handler.removeCallbacks(it)
            resetRunnable = null
        }
        isResetting = false
        isMoving = false
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
        isMoving = false
        handler.removeCallbacksAndMessages(null)
    }

}