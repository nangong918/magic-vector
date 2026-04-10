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
  final TextEditingController _accountController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();

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
    _accountController.dispose();
    _passwordController.dispose();
    _vm.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _vm,
      builder: (_, __) {
        final state = _vm.state;
        if (_accountController.text != state.account) {
          _accountController.value = TextEditingValue(
            text: state.account,
            selection: TextSelection.collapsed(offset: state.account.length),
          );
        }
        if (_passwordController.text != state.password) {
          _passwordController.value = TextEditingValue(
            text: state.password,
            selection: TextSelection.collapsed(offset: state.password.length),
          );
        }
        return Scaffold(
          appBar: AppBar(title: const Text('登录')),
          body: SafeArea(
            child: LayoutBuilder(
              builder: (context, constraints) {
                return SingleChildScrollView(
                  keyboardDismissBehavior:
                      ScrollViewKeyboardDismissBehavior.onDrag,
                  padding: const EdgeInsets.all(24),
                  child: ConstrainedBox(
                    constraints: BoxConstraints(minHeight: constraints.maxHeight),
                    child: IntrinsicHeight(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          TextField(
                            controller: _accountController,
                            decoration: const InputDecoration(labelText: '账号'),
                            onChanged: (v) =>
                                _vm.processIntent(LoginUpdateAccount(v)),
                            enableInteractiveSelection: true,
                          ),
                          if (state.savedSessions.isNotEmpty)
                            Align(
                              alignment: Alignment.centerRight,
                              child: PopupMenuButton<String>(
                                tooltip: '选择已保存账号',
                                onSelected: (v) => _vm.processIntent(
                                  LoginSelectSavedAccount(v),
                                ),
                                itemBuilder: (context) {
                                  return state.savedSessions
                                      .map(
                                        (e) => PopupMenuItem<String>(
                                          value: e.account,
                                          child: Text(e.account),
                                        ),
                                      )
                                      .toList();
                                },
                                child: const Padding(
                                  padding: EdgeInsets.symmetric(vertical: 6),
                                  child: Text('选择已保存账号'),
                                ),
                              ),
                            ),
                          const SizedBox(height: 12),
                          TextField(
                            controller: _passwordController,
                            decoration: const InputDecoration(labelText: '密码'),
                            obscureText: true,
                            onChanged: (v) =>
                                _vm.processIntent(LoginUpdatePassword(v)),
                          ),
                          const SizedBox(height: 20),
                          SizedBox(
                            width: double.infinity,
                            child: ElevatedButton(
                              onPressed: state.canSubmit
                                  ? () => _vm.processIntent(const LoginSubmit())
                                  : null,
                              child: state.isLoading
                                  ? const SizedBox(
                                      height: 18,
                                      width: 18,
                                      child: CircularProgressIndicator(
                                        strokeWidth: 2,
                                      ),
                                    )
                                  : const Text('登录'),
                            ),
                          ),
                          const SizedBox(height: 8),
                          TextButton(
                            onPressed: () =>
                                _vm.processIntent(const LoginGoRegister()),
                            child: const Text('没有账号？去注册'),
                          ),
                          TextButton(
                            onPressed: () =>
                                _vm.processIntent(const LoginTouristAccess()),
                            child: const Text('游客访问'),
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              },
            ),
          ),
        );
      },
    );
  }
}
