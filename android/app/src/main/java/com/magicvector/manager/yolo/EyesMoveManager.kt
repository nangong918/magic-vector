package com.magicvector.manager.yolo

import android.view.View
import com.detection.yolov8.targetPoint.TargetPoint

object EyesMoveManager {

    fun moveLayoutToTargetPoint(
        targetPoint: TargetPoint,
        screenWidth: Int,
        screenHeight: Int,
        layout: View
    ) {
        layout.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val layoutWidth = layout.measuredWidth
        val layoutHeight = layout.measuredHeight

        // 计算左上角坐标
        var left = (targetPoint.x * screenWidth) - (layoutWidth / 2)
        var top = (targetPoint.y * screenHeight) - (layoutHeight / 2)

        /*
            边界处理：
            left < 0 -> left = 0; left + layoutWidth > screenWidth -> left = screenWidth - layoutWidth
            top < 0 -> top = 0; top + layoutHeight > screenHeight -> top = screenHeight - layoutHeight
        */
        if (left < 0) {
            left = 0f
        } else if (left + layoutWidth > screenWidth) {
            left = (screenWidth - layoutWidth).toFloat()
        }

        if (top < 0) {
            top = 0f
        } else if (top + layoutHeight > screenHeight) {
            top = (screenHeight - layoutHeight).toFloat()
        }

        // 设置布局的位置
        layout.x = left
        layout.y = top
    }

}