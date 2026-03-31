package com.magicvector.domain.dto.http.response


/**
 * RK 设备状态上报 DTO
 */
data class RkStatusResponse(
    val deviceId: String,
    val battery: Int,
    val position: String
)