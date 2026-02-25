package com.detection.yolov8.targetPoint

import com.detection.yolov8.BoundingBox

object YOLOv8TargetPointGenerator {

    const val PERSON_CLS = 0

    /**
     * 生成全目标中心点
     * @param boundingBoxes 边界框列表
     * @return TargetPoint 全目标中心点
     */
    fun generateAllTargetPoint(boundingBoxes: List<BoundingBox>, filterSize: Float = 0f): TargetPoint {
        val resetTargetPoint = TargetPoint(
            x = 0.5F,
            y = 0.5F,
            cls = null,
            clsName = null
        )
        if (boundingBoxes.isEmpty()){
            return resetTargetPoint
        }
        else {
            var totalX = 0.0f
            var totalY = 0.0f
            var count = 0

            // 遍历所有边界框，计算中心点
            for (box in boundingBoxes) {
                // 不满足尺寸要求, 过滤
                if (box.w * box.h < filterSize) {
                    continue
                }
                val centerX = (box.x1 + box.x2) / 2
                val centerY = (box.y1 + box.y2) / 2
                totalX += centerX
                totalY += centerY
                count++
            }

            // 无数据
            if (count == 0){
                return resetTargetPoint
            }

            // 计算平均中心点
            val averageX = totalX / count
            val averageY = totalY / count

            // 返回平均中心点（类别和名称可以根据需要设置）
            return TargetPoint(
                x = averageX,
                y = averageY,
                cls = null, // 如果需要可以设定类别
                clsName = null // 如果需要可以设定类别名称
            )
        }
    }

    /**
     * 生成最大目标中心点
     * @param boundingBoxes 边界框列表
     * @return TargetPoint 最大目标中心点
     */
    fun generateMaxTargetPoint(boundingBoxes: List<BoundingBox>, filterSize: Float = 0f): TargetPoint {
        if (boundingBoxes.isEmpty()){
            return TargetPoint(
                x = 0.5F,
                y = 0.5F,
                cls = null,
                clsName = null
            )
        }
        else {
            var maxArea = 0.0f
            var maxBox: BoundingBox? = null

            // 遍历所有边界框，找到面积最大的框
            for (box in boundingBoxes) {
                // 使用 w 和 h 计算当前边界框的面积
                val area = box.w * box.h
                // 如果当前边界框的面积大于最大面积且大于指定尺寸
                if (area > maxArea && area >= filterSize) {
                    maxArea = area
                    maxBox = box
                }
            }

            // 计算最大边界框的中心点
            maxBox?.let {
                val centerX = it.x1 + it.w / 2
                val centerY = it.y1 + it.h / 2

                return TargetPoint(
                    x = centerX,
                    y = centerY,
                    cls = it.cls, // 可以根据需要设置类别
                    clsName = it.clsName // 可以根据需要设置类别名称
                )
            }

            // 如果没有找到最大边界框（理论上不应该发生）
            return TargetPoint(
                x = 0.5F,
                y = 0.5F,
                cls = null,
                clsName = null
            )
        }
    }

    /**
     * 指定物品最大目标中心点：eg: 看person；
     * @param boundingBoxes 边界框列表
     * @param specificClass 指定物品的类别索引
     */
    fun generateSpecificTargetPoint(boundingBoxes: List<BoundingBox>, specificClass: Int, filterSize: Float = 0f): TargetPoint {
        val resetTargetPoint = TargetPoint(
            x = 0.5F,
            y = 0.5F,
            cls = null,
            clsName = null
        )
        if (boundingBoxes.isEmpty() || specificClass < 0){
            return resetTargetPoint
        }
        else {
            var maxArea = 0.0f
            var maxBox: BoundingBox? = null

            // 遍历所有边界框，找到指定类别的最大框
            for (box in boundingBoxes) {
                if (box.cls == specificClass) {
                    // 使用 w 和 h 计算当前边界框的面积
                    val area = box.w * box.h
                    // 如果当前边界框的面积大于最大面积
                    if (area > maxArea && area >= filterSize) {
                        maxArea = area
                        maxBox = box
                    }
                }
            }

            // 计算最大边界框的中心点
            maxBox?.let {
                val centerX = it.x1 + it.w / 2
                val centerY = it.y1 + it.h / 2

                return TargetPoint(
                    x = centerX,
                    y = centerY,
                    cls = it.cls,
                    clsName = it.clsName
                )
            }

            // 如果没有找到指定类别的最大边界框
            return resetTargetPoint
        }
    }

    /**
     * 某物品之外的最大中心点：eg: user: 看看我手上拿的是什么东西？（这时候就要剔除某个物品其余的最大中心点[提出person]）
     * @param boundingBoxes 边界框列表
     * @param specificClass 某物品的类别索引
     * @return TargetPoint 某物品之外最大目标中心点
     */
    fun generateMaxTargetPointExcludeSpecificClass(boundingBoxes: List<BoundingBox>, specificClass: Int, filterSize: Float = 0f): TargetPoint {
        val resetTargetPoint = TargetPoint(
            x = 0.5F,
            y = 0.5F,
            cls = null,
            clsName = null
        )
        if (boundingBoxes.isEmpty()){
            return resetTargetPoint
        }
        if (specificClass < 0) {
            return generateMaxTargetPoint(boundingBoxes)
        }
        else {
            var maxArea = 0.0f
            var maxBox: BoundingBox? = null

            // 遍历所有边界框，找到不属于指定类别的最大框
            for (box in boundingBoxes) {
                if (box.cls != specificClass) { // 排除指定类别
                    // 使用 w 和 h 计算当前边界框的面积
                    val area = box.w * box.h
                    // 如果当前边界框的面积大于最大面积
                    if (area > maxArea && area >= filterSize) {
                        maxArea = area
                        maxBox = box
                    }
                }
            }

            // 计算最大边界框的中心点
            maxBox?.let {
                val centerX = it.x1 + it.w / 2
                val centerY = it.y1 + it.h / 2

                return TargetPoint(
                    x = centerX,
                    y = centerY,
                    cls = it.cls, // 可以根据需要设置类别
                    clsName = it.clsName // 可以根据需要设置类别名称
                )
            }

            // 如果没有找到不属于指定类别的最大边界框
            return resetTargetPoint
        }
    }

    /**
     * 人物优先, 其次物品, 过滤大小
     * @param boundingBoxes 边界框列表
     * @param filterSize 过滤尺寸
     * @return TargetPoint 目标中心点
     */
    fun generateTargetPoint(boundingBoxes: List<BoundingBox>, filterSize: Float = 0f): TargetPoint {
        val resetTargetPoint = TargetPoint(
            x = 0.5F,
            y = 0.5F,
            cls = null,
            clsName = null
        )
        if (boundingBoxes.isEmpty()){
            return resetTargetPoint
        }
        // 查找person
        var isHavePerson = false
        for (box in boundingBoxes) {
            if (box.cls == PERSON_CLS) {
                isHavePerson = true
                break
            }
        }
        return if (isHavePerson) {
            // 只找人
            generateSpecificTargetPoint(boundingBoxes, PERSON_CLS, filterSize)
        } else {
            // 找人之外
            generateMaxTargetPointExcludeSpecificClass(boundingBoxes, PERSON_CLS, filterSize)
        }
    }
}