import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../fragment/home_screen.dart';
import '../fragment/apply_screen.dart';
import '../fragment/mine_screen.dart';
import '../viewmodel/main_viewmodel.dart';

class MainScreen extends ConsumerStatefulWidget {
  const MainScreen({super.key});

  @override
  ConsumerState<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends ConsumerState<MainScreen> {
  // 用于管理 Effect 订阅的 StreamSubscription
  StreamSubscription<MainEffect>? _effectSubscription;

  @override
  void initState() {
    super.initState();
    // 模拟从 Intent 获取初始 Tab
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _initialize();
      _setupEffectListener();
    });
  }

  void _initialize() {
    // 这里可以从路由参数获取初始 Tab
    ref.read(mainViewModelProvider.notifier).onEvent(
      InitializeEvent(MainTab.home),
    );
  }

  void _setupEffectListener() {
    // 获取 ViewModel 并监听 Effect
    final viewModel = ref.read(mainViewModelProvider.notifier);

    // 取消之前的订阅（防止重复）
    _effectSubscription?.cancel();

    // 监听 Effect Stream
    _effectSubscription = viewModel.effect.listen((effect) {
      if (mounted) {
        _handleEffect(effect);
      }
    });
  }

  void _handleEffect(MainEffect effect) {
    if (effect is LaunchCreateAgentEffect) {
      // 跳转到创建 Agent 页面
      _navigateToCreateAgent();
    }
    // 可以处理其他 Effect
  }

  void _navigateToCreateAgent() {
    // 实现跳转逻辑
    Navigator.pushNamed(context, '/create_agent');
    // 或者使用 GoRouter
    // context.go('/create-agent');
  }

  @override
  void dispose() {
    _effectSubscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(mainViewModelProvider);
    final viewModel = ref.read(mainViewModelProvider.notifier);

    return Scaffold(
      backgroundColor: const Color(0xFFF6F7F8),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _tabToIndex(state.currentTab),
        onDestinationSelected: (index) {
          viewModel.onEvent(SelectTabEvent(_indexToTab(index)));
        },
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.home_outlined),
            selectedIcon: Icon(Icons.home),
            label: '消息列表',
          ),
          NavigationDestination(
            icon: Icon(Icons.settings_outlined),
            selectedIcon: Icon(Icons.settings),
            label: '选项',
          ),
          NavigationDestination(
            icon: Icon(Icons.person_outline),
            selectedIcon: Icon(Icons.person),
            label: '我的',
          ),
        ],
      ),
      body: SafeArea(
        child: IndexedStack(
          index: _tabToIndex(state.currentTab),
          children: [
            HomeScreen(
              isServiceBound: state.isChatServiceBound,
              onCreateAgent: () {
                viewModel.onEvent(OpenCreateAgentEvent());
              },
            ),
            const ApplyScreen(),
            MineScreen(isServiceBound: state.isChatServiceBound),
          ],
        ),
      ),
    );
  }

  int _tabToIndex(MainTab tab) {
    switch (tab) {
      case MainTab.home:
        return 0;
      case MainTab.apply:
        return 1;
      case MainTab.mine:
        return 2;
    }
  }

  MainTab _indexToTab(int index) {
    switch (index) {
      case 0:
        return MainTab.home;
      case 1:
        return MainTab.apply;
      case 2:
        return MainTab.mine;
      default:
        return MainTab.home;
    }
  }
}