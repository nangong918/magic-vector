package com.data.domain.ao.yolo

data class TargetActivityDetectionResult(
    val result: Boolean,
    val score: Float,
    // null: 无结果, 0: person, 1: object
    val detectionType: Int? = null
)
