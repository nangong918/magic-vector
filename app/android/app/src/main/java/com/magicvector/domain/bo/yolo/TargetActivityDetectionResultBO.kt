package com.magicvector.domain.bo.yolo

data class TargetActivityDetectionResultBO(
    val result: Boolean,
    val score: Float,
    // null: 无结果, 0: person, 1: object
    val detectionType: Int? = null
)
