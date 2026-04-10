import 'dart:async';

import 'package:flutter/material.dart';

import '../config/app_route.dart';
import '../viewmodel/start_vm.dart';

class StartPage extends StatefulWidget {
  const StartPage({super.key});

  @override
  State<StartPage> createState() => _StartPageState();
}

class _StartPageState extends State<StartPage> {
  late final StartVm _vm;
  StreamSubscription<StartEffect>? _effectSub;

  @override
  void initState() {
    super.initState();
    _vm = StartVm();
    _effectSub = _vm.effects.listen(_consumeEffect);
    _vm.processIntent(const StartInitialize());
  }

  void _consumeEffect(StartEffect effect) {
    if (!mounted) return;
    if (effect is StartNavigateToMain) {
      Navigator.pushReplacementNamed(context, AppRoutes.main);
    } else if (effect is StartNavigateToLogin) {
      Navigator.pushReplacementNamed(context, AppRoutes.login);
    } else if (effect is StartShowToast) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(effect.message)));
    }
  }

  @override
  void dispose() {
    _effectSub?.cancel();
    _vm.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _vm,
      builder: (_, __) {
        return Scaffold(
          body: Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const FlutterLogo(size: 120),
                const SizedBox(height: 16),
                Text(
                  _vm.state.isLoading ? '启动中...' : '即将进入',
                  style: const TextStyle(fontSize: 16),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
