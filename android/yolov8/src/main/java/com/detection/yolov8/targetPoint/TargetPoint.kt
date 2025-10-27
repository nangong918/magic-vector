package com.detection.yolov8.targetPoint

/**
 * 目标点数据类型。
 *
 * 根据BoundingBox结果，生成视觉注视中心点。
 * @see com.detection.yolov8.BoundingBox
 *
 * @property x 目标点的 x 坐标（通常表示在图像或屏幕上的位置）。
 * @property y 目标点的 y 坐标（通常表示在图像或屏幕上的位置）。
 * @property cls 目标点类别的索引，指示目标属于哪个类别。全目标的情况下为null
 * @property clsName 目标点的类别名称，例如 "laptop"、"person" 等。全目标的情况下为null
 *
 * * 示例 JSON:
 *
 *  {
 *
 *      "x": 0.5,
 *
 *      "y": 0.6,
 *
 *      "cls": 1,
 *
 *      "clsName": "person"
 *
 *  }
 *
 */
data class TargetPoint(
    val x: Float,
    val y: Float,
    val cls: Int?,
    val clsName: String?
)