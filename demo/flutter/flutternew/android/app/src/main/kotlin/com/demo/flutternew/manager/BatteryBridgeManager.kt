package com.demo.flutternew.manager

import android.content.Context
import com.demo.aarlib.BatteryNativeBridge
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class BatteryBridgeManager(private val context: Context) {
    private val BATTERY_CHANNEL = "com.demo.flutternew/battery"

    fun registerWith(flutterEngine: FlutterEngine) {
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, BATTERY_CHANNEL).setMethodCallHandler {
            call, result ->
            when (call.method) {
                "getBatteryLevel" -> {
                    val batteryLevel = getBatteryLevel()
                    if (batteryLevel != -1) {
                        result.success(batteryLevel)
                    } else {
                        result.error("UNAVAILABLE", "电池信息不可用", null)
                    }
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    private fun getBatteryLevel(): Int {
        return BatteryNativeBridge.getBatteryLevel(context)
    }
}
