package com.detection.yolov8

/**
 * 数据类表示目标检测中的边界框及其相关信息。
 * 
 * 数据都是浮点数，意味着它们是相对于整个图像或屏幕的尺寸来计算的
 * 
 * @property x1 边界框左上角的 x 坐标（归一化值，范围 0 到 1）。
 * @property y1 边界框左上角的 y 坐标（归一化值，范围 0 到 1）。
 * @property x2 边界框右下角的 x 坐标（归一化值，范围 0 到 1）。
 * @property y2 边界框右下角的 y 坐标（归一化值，范围 0 到 1）。
 * @property cx 边界框中心点的 x 坐标（归一化值，通常为 (x1 + x2) / 2）。
 * @property cy 边界框中心点的 y 坐标（归一化值，通常为 (y1 + y2) / 2）。
 * @property w 边界框的宽度（归一化值，通常为 x2 - x1）。
 * @property h 边界框的高度（归一化值，通常为 y2 - y1）。
 * @property cnf 置信度分数，表示模型对检测结果的置信度（范围 0 到 1）。
 * @property cls 检测到的对象类别的索引，用于标识类别。
 * @property clsName 检测到的对象的类别名称，例如 "laptop"、"person" 等。
 *
 *  * 示例 JSON:
 *
 *  {
 *
 *      "cls": 63,
 *
 *      "clsName": "laptop",
 *
 *      "cnf": 0.7466302,
 *
 *      "cx": 0.5550764,
 *
 *      "cy": 0.49846858,
 *
 *      "h": 0.9906417,
 *
 *      "w": 0.8877679,
 *
 *      "x1": 0.111192465,
 *
 *      "x2": 0.9989604,
 *
 *      "y1": 0.0031477213,
 *
 *      "y2": 0.99378943
 *
 *  }
 *
 */
data class BoundingBox(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val cx: Float,
    val cy: Float,
    val w: Float,
    val h: Float,
    val cnf: Float,
    val cls: Int,
    val clsName: String
)