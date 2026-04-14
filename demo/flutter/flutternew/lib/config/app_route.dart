import 'package:flutter/material.dart';
import 'package:flutternew/page/native_call_page.dart';
import 'package:flutternew/page/network_page.dart';
import 'package:flutternew/page/offline_ivw_page.dart';
import 'package:flutternew/page/register_page.dart';
import 'package:flutternew/page/start_page.dart';
import 'package:flutternew/page/vad_page.dart';
import 'package:flutternew/page/voice_agent_page.dart';
import 'package:flutternew/page/xfyun_stt_page.dart';
import 'package:flutternew/page/login_page.dart';

import '../page/chat_page.dart';
import '../page/main_page.dart';
import '../page/oss_demo_page.dart';

class AppRoutes {
  // 路由名称常量（避免字符串硬编码）
  static const String main = '/';         // 主页面（根路由）
  static const String start = '/start';
  static const String login = '/login';
  static const String register = '/register';
  static const String chat = '/chat';     // 聊天页
  static const String network = '/network'; // 网络页
  static const String native = '/native'; // 原生页
  static const String xfyunStt = '/xfyun-stt'; // 讯飞语音识别
  static const String offlineIvw = '/offline-ivw'; // 离线语音唤醒
  static const String vad = '/vad'; // VAD测试页
  static const String voiceAgent = '/voice-agent'; // 语音助手
  static const String ossDemo = '/oss-demo';
}


// 核心：路由Map（替代onGenerateRoute，对应你要的appRoutes形式）
final Map<String, WidgetBuilder> appRoutes = {
  AppRoutes.start: (BuildContext context) {
    return const StartPage();
  },

  AppRoutes.login: (BuildContext context) {
    return const LoginPage();
  },

  AppRoutes.register: (BuildContext context) {
    return const RegisterPage();
  },

  // 主页面（无参数）
  AppRoutes.main: (BuildContext context) {
    return const MainPage();
  },

  // 聊天演示页（接收参数）
  AppRoutes.chat: (BuildContext context) {
    // 获取跳转参数
    final arguments = ModalRoute.of(context)?.settings.arguments as Map<String, dynamic>?;
    return ChatPage(
      title: arguments?['title'] ?? 'Chat Demo',
      description: arguments?['description'] ?? '聊天列表演示',
    );
  },

  // 原生页面
  AppRoutes.native: (BuildContext context) {
    return const NativeCallPage();
  },

  // 网络页面
  AppRoutes.network: (BuildContext context) {
    return const NetworkPage();
  },

  // 讯飞语音识别
  AppRoutes.xfyunStt: (BuildContext context) {
    return const XfYunSttPage();
  },

  // 离线语音唤醒
  AppRoutes.offlineIvw: (BuildContext context) {
    return const OfflineIvwPage();
  },

  // VAD测试页
  AppRoutes.vad: (BuildContext context) {
    return const VadPage();
  },

  // 语音助手页
  AppRoutes.voiceAgent: (BuildContext context) {
    return const VoiceAgentPage();
  },

  AppRoutes.ossDemo: (BuildContext context) {
    return const OssDemoPage();
  },
};


// 可选：未知路由处理（如果需要全局404，保留onGenerateRoute兜底）
Route<dynamic> unknownRoute(RouteSettings settings) {
  return MaterialPageRoute(
    builder: (context) =>
        Scaffold(
          appBar: AppBar(title: const Text('页面不存在')),
          body: Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Text('⚠️ 未找到指定页面'),
                const SizedBox(height: 20),
                ElevatedButton(
                  onPressed: () => Navigator.pushNamed(context, AppRoutes.main),
                  child: const Text('返回首页'),
                ),
              ],
            ),
          ),
        ),
  );
}
