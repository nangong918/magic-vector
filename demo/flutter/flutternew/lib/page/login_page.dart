import 'dart:async';

import 'package:flutter/material.dart';

import '../config/app_route.dart';
import '../viewmodel/login_vm.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  late final LoginVm _vm;
  StreamSubscription<LoginEffect>? _effectSub;

  @override
  void initState() {
    super.initState();
    _vm = LoginVm();
    _effectSub = _vm.effects.listen(_consumeEffect);
  }

  void _consumeEffect(LoginEffect effect) {
    if (!mounted) return;
    if (effect is LoginNavigateToMain) {
      Navigator.pushNamedAndRemoveUntil(context, AppRoutes.main, (route) => false);
    } else if (effect is LoginNavigateToRegister) {
      Navigator.pushNamed(context, AppRoutes.register);
    } else if (effect is LoginShowToast) {
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
          appBar: AppBar(title: const Text('登录')),
          body: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                TextField(
                  decoration: const InputDecoration(labelText: '账号'),
                  onChanged: (v) => _vm.processIntent(LoginUpdateAccount(v)),
                ),
                const SizedBox(height: 12),
                TextField(
                  decoration: const InputDecoration(labelText: '密码'),
                  obscureText: true,
                  onChanged: (v) => _vm.processIntent(LoginUpdatePassword(v)),
                ),
                const SizedBox(height: 20),
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: state.canSubmit ? () => _vm.processIntent(const LoginSubmit()) : null,
                    child: state.isLoading
                        ? const SizedBox(
                            height: 18,
                            width: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Text('登录'),
                  ),
                ),
                const SizedBox(height: 8),
                TextButton(
                  onPressed: () => _vm.processIntent(const LoginGoRegister()),
                  child: const Text('没有账号？去注册'),
                ),
                TextButton(
                  onPressed: () => _vm.processIntent(const LoginTouristAccess()),
                  child: const Text('游客访问'),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
