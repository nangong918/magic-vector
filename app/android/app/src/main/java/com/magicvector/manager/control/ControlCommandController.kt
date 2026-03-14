package com.magicvector.manager.control

import com.data.domain.dto.request.ControlCommandRequest
import com.magicvector.MainApplication
import java.util.concurrent.atomic.AtomicLong

/**
 * 控制指令编排器（Controller）：
 * 负责把摇杆/按钮输入转换为统一请求结构。
 */
class ControlCommandController {
    private val sequenceCounter = AtomicLong(0L)

    fun buildJoystickCommand(
        userId: String,
        deviceId: String,
        transport: String,
        key: String,
        x: Float,
        y: Float
    ): ControlCommandRequest {
        val payload = mapOf(
            "key" to key,
            "x" to x,
            "y" to y
        )
        return baseRequest(
            userId = userId,
            deviceId = deviceId,
            transport = transport,
            commandType = "JOYSTICK",
            payloadJson = MainApplication.GSON.toJson(payload)
        )
    }

    fun buildButtonCommand(
        userId: String,
        deviceId: String,
        transport: String,
        action: String
    ): ControlCommandRequest {
        val payload = mapOf("action" to action)
        return baseRequest(
            userId = userId,
            deviceId = deviceId,
            transport = transport,
            commandType = "BUTTON",
            payloadJson = MainApplication.GSON.toJson(payload)
        )
    }

    private fun baseRequest(
        userId: String,
        deviceId: String,
        transport: String,
        commandType: String,
        payloadJson: String
    ): ControlCommandRequest {
        return ControlCommandRequest().apply {
            this.userId = userId
            this.deviceId = deviceId
            this.transport = transport
            this.commandType = commandType
            this.sequence = sequenceCounter.getAndIncrement()
            this.timestamp = System.currentTimeMillis()
            this.payloadJson = payloadJson
        }
    }
}
