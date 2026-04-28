import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutternew/service/live_android_service.dart';

class LivePushAndroidPage extends StatefulWidget {
  const LivePushAndroidPage({super.key});

  @override
  State<LivePushAndroidPage> createState() => _LivePushAndroidPageState();
}

class _LivePushAndroidPageState extends State<LivePushAndroidPage> {
  String _status = '等待启动 Live Push Demo';

  Future<void> _openNativeDemo() async {
    if (!Platform.isAndroid) {
      setState(() {
        _status = '当前平台不是 Android，无法启动原生推流页面';
      });
      return;
    }
    try {
      await LiveAndroidService.openLivePushDemo();
      setState(() {
        _status = '已打开 Live Push Demo (Android)';
      });
    } on PlatformException catch (e) {
      setState(() {
        _status = '打开失败: ${e.message}';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Live Push Demo (Android)')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '该页面调用 Android 原生 Activity 完成 RTMP + x264 推流。\n'
              '请确保先启动推流服务器，再点击下方按钮。',
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: _openNativeDemo,
              child: const Text('打开 Live Push Demo (Android)'),
            ),
            const SizedBox(height: 16),
            Text('状态: $_status'),
          ],
        ),
      ),
    );
  }
}
