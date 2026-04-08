// lib/main.dart
import 'package:flutter/material.dart';
import 'package:flutter_native_splash/flutter_native_splash.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:vector/domain/constant/base_constant.dart';
import 'package:vector/page/main_page.dart';
import 'package:vector/viewmodel/main_viewmodel.dart';

void main() {
  WidgetsBinding widgetsBinding = WidgetsFlutterBinding.ensureInitialized();
  FlutterNativeSplash.preserve(widgetsBinding: widgetsBinding);
  runApp(const ProviderScope(child: MyApp()));
}

class MyApp extends ConsumerStatefulWidget {
  const MyApp({super.key});

  @override
  ConsumerState<MyApp> createState() => _MyAppState();
}

class _MyAppState extends ConsumerState<MyApp> {
  @override
  void initState() {
    super.initState();
    _initialize();
  }

  Future<void> _initialize() async {
    // 模拟加载
    await Future.delayed(
      Duration(milliseconds: BaseConstant.constant.startDelayTime),
    );

    // 移除闪屏
    FlutterNativeSplash.remove();

    // 监听 MainViewModel 的 Effect
    final viewModel = ref.read(mainViewModelProvider.notifier);
    viewModel.effect.listen((effect) {
      if (effect is LaunchCreateAgentEffect) {
        // 跳转到创建 Agent 页面
        _navigateToCreateAgent();
      }
    });
  }

  void _navigateToCreateAgent() {
    // 实现跳转到创建 Agent 页面的逻辑
    // Navigator.pushNamed(context, '/create_agent');
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'vector',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const MainScreen(),
      routes: {
        // '/create_agent': (context) => const CreateAgentScreen(),
        // '/test': (context) => const TestScreen(),
      },
    );
  }
}