package com.demo.flutter3_app

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {
    // 1. 电量Channel（原有）
    private val BATTERY_CHANNEL = "com.demo.flutter3_app/battery"
    // 2. 新增WiFi Channel（需与Flutter端一致）
    private val WIFI_CHANNEL = "com.demo.flutter3_app/wifi"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // ========== 原有：电量调用逻辑 ==========
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, BATTERY_CHANNEL).setMethodCallHandler { call, result ->
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

        // ========== 新增：WiFi信号强度调用逻辑 ==========
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, WIFI_CHANNEL).setMethodCallHandler { call, result ->
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

    // ========== 原有：获取电池电量 ==========
    private fun getBatteryLevel(): Int {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    // ========== 新增：获取WiFi信号强度（返回dBm值，负数，如-65） ==========
    private fun getWifiSignalStrength(): Int {
        // 1. 获取WifiManager实例
        val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager

        // 2. 检查WiFi是否开启
        if (!wifiManager.isWifiEnabled) {
            return -999 // 未开启WiFi，返回兜底值
        }

        // 3. 获取当前WiFi连接信息
        val wifiInfo: WifiInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 推荐用法
            wifiManager.connectionInfo
        } else {
            // 低版本兼容
            wifiManager.connectionInfo
        }

        // 4. 获取信号强度（RSSI：Received Signal Strength Indicator，单位dBm）
        return wifiInfo?.rssi ?: -999 // 未连接WiFi返回-999
    }

    // ========== 可选：判断WiFi是否已连接 ==========
    private fun isWifiConnected(): Boolean {
        val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifiManager.isWifiEnabled) return false

        val wifiInfo = wifiManager.connectionInfo
        // 判断是否已分配IP（已连接的标志）
        return wifiInfo.ipAddress != 0
    }
}