package com.demo.flutternew.manager

import android.content.Context
import com.demo.aarlib.WifiNativeBridge
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class WifiBridgeManager(private val context: Context) {
    private val WIFI_CHANNEL = "com.demo.flutternew/wifi"

    fun registerWith(flutterEngine: FlutterEngine) {
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, WIFI_CHANNEL).setMethodCallHandler {
            call, result ->
            when (call.method) {
                // Flutter端调用的方法名：getWifiSignalStrength
                "getWifiSignalStrength" -> {
                    val signalStrength = getWifiSignalStrength()
                    if (signalStrength != -999) { // 自定义兜底值
                        result.success(signalStrength)
                    } else {
                        result.error("UNAVAILABLE", "WiFi信号强度获取失败", null)
                    }
                }
                // 可选：新增获取WiFi是否连接的方法
                "isWifiConnected" -> {
                    val isConnected = isWifiConnected()
                    result.success(isConnected)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    private fun getWifiSignalStrength(): Int {
        return WifiNativeBridge.getWifiSignalStrength(context)
    }

    private fun isWifiConnected(): Boolean {
        return WifiNativeBridge.isWifiConnected(context)
    }
}
