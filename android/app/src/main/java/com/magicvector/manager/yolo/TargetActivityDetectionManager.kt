package com.magicvector.manager.yolo

import com.core.baseutil.debug.DebugEnvironment
import com.detection.yolov8.BoundingBox
import com.detection.yolov8.targetPoint.TargetPoint
import com.magicvector.MainApplication
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt


/**
 * 目标活动检测: 目前是纯代码实现, 后面改为tflite模型实现
 * 是否增加/减少物体n_obj: 1 + (n * 0.05) 权重较小: 因为相同的静态图片两次检测也会出现不同的结果, 会误判
 * 是否出现了新的人物n_person: 1 + (n * 0.1)
 * 最大物面积品切换s_obj: 100 * abs(s2 - s1) * k 权重小: 1. 因为人的权重最高 2. 因为静态图片再次识别, 面积也可能再次变化
 * 最大人物面积切换s_person: 100 * abs(s2 - s1) * k 权重小: 1. 因为人的权重最高 2. 因为静态图片再次识别, 面积也可能再次变化
 * 最大面积物品远距离移动: 远距离移动: 中心点移动距离要超过整个屏幕的33% (超参数, 可调整)
 * Person是否远距离移动移动: 远距离移动: 中心点移动距离要超过整个屏幕的33% (超参数, 可调整)
 * m_obj: [abs(m)...n] * 10 * k
 * m_person: [abs(m)...n] * 10 * k
 * 倒计时Runnable, 在指定时间没有移动则是为静止状态
 * 类似VAD: 起始 -> 结束
 */
object TargetActivityDetectionManager {

    private const val TAG = "TargetActivityDetectionManager"

    // 上一帧的 最大物品面积
    private var lastMaxObjS: Float = 0f
    private var lastMaxPersonS: Float = 0f

    // 上一帧中心点
    private var lastObjTargetPoint: TargetPoint? = TargetPoint(0.5f, 0.5f, null, null)
    private var lastPersonTargetPoint: TargetPoint? = TargetPoint(0.5f, 0.5f, null, null)

    // 数量变化
    private val lastBoxCountMap = mutableMapOf<Int, Int>()

    fun detect(
        boundingBoxes: List<BoundingBox>?,
        objectTargetPoint: TargetPoint?,
        personTargetPoint: TargetPoint?
    ) {

        // 计算位移差
        val objDistance = if (objectTargetPoint == null){
            0f
        } else {
            getDistance(lastObjTargetPoint, objectTargetPoint)
        }
        val personDistance = if (personTargetPoint == null){
            0f
        } else {
            getDistance(lastPersonTargetPoint, personTargetPoint)
        }

        // 计算对象数量差距
        val boxCountDifferences = calculateBoxCountDifference(boundingBoxes)

        // 计算面积差
        var objSdiff = 0f
        var personSdiff = 0f

        var maxObjS = 0f
        var maxPersonS = 0f

        // 获取最大物品面积
        boundingBoxes?.let {
            var isHaveObj = false
            var isHavePerson = false

            for (box in it) {
                if (box.cls == 0) {
                    isHavePerson = true
                    val personS = box.w * box.h
                    if (personS > maxPersonS){
                        maxPersonS = personS
                    }
                }
                else {
                    isHaveObj = true
                    val objS = box.w * box.h
                    if (objS > maxObjS){
                        maxObjS = objS
                    }
                }
            }

            // 计算面积差
            objSdiff = abs(maxObjS - lastMaxObjS)
            personSdiff = abs(maxPersonS - lastMaxPersonS)

            // 记录
            lastMaxObjS = maxObjS
            lastMaxPersonS = maxPersonS
        }

        if (DebugEnvironment.LOG_DEBUG) {
            println("$TAG::detect")
            println("$TAG: objDistance: $objDistance")
            println("$TAG: personDistance: $personDistance")
            println("$TAG: boxCountDifferences: ${MainApplication.GSON.toJson(boxCountDifferences)}")
            println("$TAG: objSdiff: $objSdiff")
            println("$TAG: personSdiff: $personSdiff")
        }

        // 最后记录
        lastObjTargetPoint = objectTargetPoint
        lastPersonTargetPoint = personTargetPoint
        lastMaxObjS = maxObjS
        lastMaxPersonS = maxPersonS
    }

    // 计算距离
    private fun getDistance(startPoint: TargetPoint?, endPoint: TargetPoint): Float{
        if (startPoint == null){
            return getDistance(TargetPoint(0.5f, 0.5f, null, null), endPoint)
        }
        val s_x = startPoint.x
        val s_y = startPoint.y
        val e_x = endPoint.x
        val e_y = endPoint.y
        return sqrt((s_x - e_x).toDouble().pow(2.0) + (s_y - e_y).toDouble().pow(2.0)).toFloat()
    }

    // 计算物品数量差距
    private fun calculateBoxCountDifference(boundingBoxes: List<BoundingBox>?): List<Int>{
        // 0: person; 1: objects
        val boxCountDifference = mutableListOf(0, 0)

        if (boundingBoxes == null){
            // 既然不存在, 说明跟lastMap存在差距
            // for循环lastBoxCountMap
            for ((key, value) in lastBoxCountMap){
                if (key == 0){
                    boxCountDifference[0] = boxCountDifference[0] + value
                }
                else {
                    boxCountDifference[1] = boxCountDifference[1] + value
                }
            }
        }
        else {
            // List<BoundingBox> -> mutableMapOf<Int, Int>()
            val boxCountMap = mutableMapOf<Int, Int>()
            for (box in boundingBoxes) {
                boxCountMap[box.cls] = boxCountMap.getOrDefault(box.cls, 0) + 1
            }

            // 存在则对比lastBoxCountMap和boundingBoxes的差距
            for ((key, value) in boxCountMap){
                val lastValue = lastBoxCountMap.getOrDefault(key, 0)
                if (key == 0) {
                    boxCountDifference[0] = boxCountDifference[0] + abs(lastValue - value)
                }
                else {
                    boxCountDifference[1] = boxCountDifference[1] + abs(lastValue - value)
                }
            }
        }

        return boxCountDifference
    }
}