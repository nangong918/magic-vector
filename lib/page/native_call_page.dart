import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// Flutter调用Android，IOS，Ohos原生的demo
class NativeCallPage extends StatefulWidget {
  const NativeCallPage({super.key});

  @override
  State<NativeCallPage> createState() => _NativeCallPageState();
}

class _NativeCallPageState extends State<NativeCallPage> {
  // 模拟电量数据（实际需通过MethodChannel调用原生获取）
  String _batteryLevel = "未获取";
  // WiFi信号强度数据
  String _wifiSignalStrength = "未获取";

  // ========== 电量相关 MethodChannel ==========
  static const batteryPlatform = MethodChannel('com.demo.flutter3_app/battery');
  // ========== WiFi相关 MethodChannel（与Android原生端保持一致） ==========
  static const wifiPlatform = MethodChannel('com.demo.flutter3_app/wifi');

  // 调用Android原生获取电量
  Future<void> _callAndroidNative() async {
    try {
      // 调用Android原生方法
      final int result = await batteryPlatform.invokeMethod('getBatteryLevel');
      setState(() {
        _batteryLevel = "Android原生返回：$result%";
      });
    } on PlatformException catch (e) {
      // 处理调用失败的情况
      setState(() {
        _batteryLevel = "调用失败: '${e.message}'";
      });
    }
  }

  // 调用iOS原生获取电量（暂时保留）
  Future<void> _callIOSNative() async {
    // 这里仅模拟，实际需替换为MethodChannel调用iOS原生代码
    setState(() {
      _batteryLevel = "iOS原生返回：92%";
    });
  }

  // ========== 新增：调用Android原生获取WiFi信号强度 ==========
  Future<void> _callAndroidWifiNative() async {
    try {
      // 调用Android原生WiFi信号强度方法
      final int result = await wifiPlatform.invokeMethod('getWifiSignalStrength');
      // 转换信号强度为更易读的格式（dBm值 + 信号等级）
      String signalDesc = _getWifiSignalDesc(result);
      setState(() {
        _wifiSignalStrength = "Android原生返回：$result dBm ($signalDesc)";
      });
    } on PlatformException catch (e) {
      // 处理调用失败的情况
      setState(() {
        _wifiSignalStrength = "WiFi调用失败: '${e.message}'";
      });
    }
  }

  // ========== 新增：调用iOS原生获取WiFi信号强度（模拟） ==========
  Future<void> _callIOSWifiNative() async {
    // 模拟iOS原生返回WiFi信号强度
    setState(() {
      _wifiSignalStrength = "iOS原生返回：-70 dBm (信号良好)";
    });
  }

  // 辅助方法：将WiFi信号强度dBm值转换为文字描述
  String _getWifiSignalDesc(int rssi) {
    if (rssi >= -50) {
      return "信号极好";
    } else if (rssi >= -60) {
      return "信号很好";
    } else if (rssi >= -70) {
      return "信号良好";
    } else if (rssi >= -80) {
      return "信号一般";
    } else if (rssi >= -90) {
      return "信号差";
    } else if (rssi >= -100) {
      return "信号极差";
    } else {
      return "未连接WiFi";
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      // AppBar
      appBar: AppBar(
        title: const Text("原生调用示例"),
        centerTitle: true,
      ),
      // 主体内容改为SingleChildScrollView实现滚动
      body: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 30),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            // ========== 电量相关区域 ==========
            const Text(
              "原生调用电量数据",
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
                color: Colors.black87,
              ),
            ),
            // 标题和第一个按钮的间距20
            const SizedBox(height: 20),
            // Android原生电量按钮
            ElevatedButton(
              onPressed: Platform.isAndroid ? _callAndroidNative : null,
              style: ElevatedButton.styleFrom(
                // 按钮宽度撑满
                minimumSize: const Size(double.infinity, 50),
                // 按钮圆角
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
              child: const Text(
                "Android原生",
                style: TextStyle(fontSize: 16),
              ),
            ),
            // 两个按钮之间的间距20
            const SizedBox(height: 20),
            // iOS原生电量按钮
            ElevatedButton(
              onPressed: Platform.isIOS ? _callIOSNative : null,
              style: ElevatedButton.styleFrom(
                minimumSize: const Size(double.infinity, 50),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
              child: const Text(
                "iOS原生",
                style: TextStyle(fontSize: 16),
              ),
            ),
            // 显示获取到的电量数据
            const SizedBox(height: 20),
            Text(
              "当前电量：$_batteryLevel",
              style: const TextStyle(fontSize: 16, color: Colors.grey),
            ),

            // ========== 分割线 + 间距 ==========
            const SizedBox(height: 40),
            const Divider(
              height: 1,
              color: Colors.white,
              thickness: 1,
            ),
            const SizedBox(height: 40),

            // ========== 新增：WiFi相关区域 ==========
            const Text(
              "原生调用WiFi信号强度",
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
                color: Colors.black87,
              ),
            ),
            // 标题和WiFi按钮间距20
            const SizedBox(height: 20),
            // Android原生WiFi按钮
            ElevatedButton(
              onPressed: Platform.isAndroid ? _callAndroidWifiNative : null,
              style: ElevatedButton.styleFrom(
                minimumSize: const Size(double.infinity, 50),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
                // 区分WiFi按钮样式（可选）
                backgroundColor: Colors.lightBlueAccent.shade700,
              ),
              child: const Text(
                "Android原生WiFi",
                style: TextStyle(fontSize: 16, color: Colors.white),
              ),
            ),
            // 两个WiFi按钮间距20
            const SizedBox(height: 20),
            // iOS原生WiFi按钮
            ElevatedButton(
              onPressed: Platform.isIOS ? _callIOSWifiNative : null,
              style: ElevatedButton.styleFrom(
                minimumSize: const Size(double.infinity, 50),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
                backgroundColor: Colors.lightBlueAccent.shade700,
              ),
              child: const Text(
                "iOS原生WiFi",
                style: TextStyle(fontSize: 16, color: Colors.white),
              ),
            ),
            // 显示WiFi信号强度数据
            const SizedBox(height: 20),
            Text(
              "WiFi信号强度：$_wifiSignalStrength",
              style: const TextStyle(fontSize: 16, color: Colors.grey),
              textAlign: TextAlign.center,
            ),

            // 底部留白，避免内容贴底
            const SizedBox(height: 50),
          ],
        ),
      ),
    );
  }
}