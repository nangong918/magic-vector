import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutternew/service/live_android_service.dart';

class LivePullAndroidPage extends StatefulWidget {
  const LivePullAndroidPage({super.key});

  @override
  State<LivePullAndroidPage> createState() => _LivePullAndroidPageState();
}

class _LivePullAndroidPageState extends State<LivePullAndroidPage> {
  String _status = '等待启动 Live Pull Demo';

  Future<void> _openNativeDemo() async {
    if (!Platform.isAndroid) {
      setState(() {
        _status = '当前平台不是 Android，无法启动原生拉流页面';
      });
      return;
    }
    try {
      await LiveAndroidService.openLivePullDemo();
      setState(() {
        _status = '已打开 Live Pull Demo (Android)';
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
      appBar: AppBar(title: const Text('Live Pull Demo (Android)')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '该页面调用 Android 原生 Activity 完成 RTMP/HLS 拉流播放（ExoPlayer）。\n'
              '你可先启动 Live Push，再来此页验证播放。',
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: _openNativeDemo,
              child: const Text('打开 Live Pull Demo (Android)'),
            ),
            const SizedBox(height: 16),
            Text('状态: $_status'),
          ],
        ),
      ),
    );
  }
}
