package com.demo.flutter3_app

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {
    // 定义MethodChannel名称，必须与Flutter端保持一致
    private val CHANNEL = "com.demo.flutter3_app/battery"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // 创建MethodChannel
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            // 处理Flutter端的方法调用
            when (call.method) {
                "getBatteryLevel" -> {
                    // 调用获取电池电量的方法
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

    // 获取当前电池电量百分比
    private fun getBatteryLevel(): Int {
        // 使用BatteryManager获取电池信息
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
}