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
  String _status = '正在打开 Live Pull Demo (Android)...';

  @override
  void initState() {
    super.initState();
    _openNativeDemo();
  }

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
      body: Center(child: Text('状态: $_status')),
    );
  }
}
