import 'dart:async';

import 'package:flutter/material.dart';

import '../config/app_route.dart';
import '../viewmodel/register_vm.dart';

class RegisterPage extends StatefulWidget {
  const RegisterPage({super.key});

  @override
  State<RegisterPage> createState() => _RegisterPageState();
}

class _RegisterPageState extends State<RegisterPage> {
  late final RegisterVm _vm;
  StreamSubscription<RegisterEffect>? _effectSub;

  @override
  void initState() {
    super.initState();
    _vm = RegisterVm();
    _effectSub = _vm.effects.listen(_consumeEffect);
  }

  void _consumeEffect(RegisterEffect effect) {
    if (!mounted) return;
    if (effect is RegisterNavigateToMain) {
      Navigator.pushNamedAndRemoveUntil(context, AppRoutes.main, (route) => false);
    } else if (effect is RegisterNavigateToLogin) {
      Navigator.pop(context);
    } else if (effect is RegisterShowToast) {
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
        final state = _vm.state;
        return Scaffold(
          appBar: AppBar(title: const Text('注册')),
          body: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                TextField(
                  decoration: const InputDecoration(labelText: '账号'),
                  onChanged: (v) => _vm.processIntent(RegisterUpdateAccount(v)),
                ),
                const SizedBox(height: 12),
                TextField(
                  decoration: const InputDecoration(labelText: '密码'),
                  obscureText: true,
                  onChanged: (v) => _vm.processIntent(RegisterUpdatePassword(v)),
                ),
                const SizedBox(height: 12),
                TextField(
                  decoration: const InputDecoration(labelText: '确认密码'),
                  obscureText: true,
                  onChanged: (v) => _vm.processIntent(RegisterUpdateConfirmPassword(v)),
                ),
                const SizedBox(height: 20),
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: state.canSubmit ? () => _vm.processIntent(const RegisterSubmit()) : null,
                    child: state.isLoading
                        ? const SizedBox(
                            height: 18,
                            width: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Text('注册'),
                  ),
                ),
                const SizedBox(height: 8),
                TextButton(
                  onPressed: () => _vm.processIntent(const RegisterGoLogin()),
                  child: const Text('已有账号？去登录'),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
