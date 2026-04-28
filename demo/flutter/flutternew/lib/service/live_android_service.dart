import 'package:flutter/services.dart';

class LiveAndroidService {
  static const MethodChannel _channel = MethodChannel(
    'com.demo.flutternew/live_demo_android',
  );

  static Future<void> openLivePushDemo() async {
    await _channel.invokeMethod('openLivePushDemo');
  }

  static Future<void> openLivePullDemo() async {
    await _channel.invokeMethod('openLivePullDemo');
  }
}
