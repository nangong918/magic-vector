

import 'dart:io';

import 'package:flutter/material.dart';

/// Flutter调用Android，IOS，Ohos原生的demo
class NativeCallPage extends StatefulWidget {

  const NativeCallPage({super.key});


  @override
  State<NativeCallPage> createState() => _NativeCallPageState();

}


class _NativeCallPageState extends State<NativeCallPage> {
  // 模拟电量数据（实际需通过MethodChannel调用原生获取）
  String _batteryLevel = "未获取";

  // 调用Android原生获取电量
  Future<void> _callAndroidNative() async {
    // 这里仅模拟，实际需替换为MethodChannel调用Android原生代码
    setState(() {
      _batteryLevel = "Android原生返回：85%";
    });
  }

  // 调用iOS原生获取电量
  Future<void> _callIOSNative() async {
    // 这里仅模拟，实际需替换为MethodChannel调用iOS原生代码
    setState(() {
      _batteryLevel = "iOS原生返回：92%";
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      // AppBar
      appBar: AppBar(
        title: const Text("原生调用示例"),
        centerTitle: true,
      ),
      // 主体内容
      body: Padding(
        // 给整体加左右内边距，避免内容贴边
        padding: const EdgeInsets.symmetric(horizontal: 20),
        child: Column(
          // 整体垂直居中
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // 标题：原生调用电量数据
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
            // Android原生按钮
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
            // iOS原生按钮
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
            // 可选：显示获取到的电量数据
            const SizedBox(height: 20),
            Text(
              "当前电量：$_batteryLevel",
              style: const TextStyle(fontSize: 16, color: Colors.grey),
            ),
          ],
        ),
      ),
    );
  }
}








