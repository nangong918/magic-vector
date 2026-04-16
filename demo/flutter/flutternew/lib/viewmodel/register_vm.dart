import 'dart:async';

import 'package:flutter/foundation.dart';

import '../domain/model/user_session_model.dart';
import '../manager/app_session.dart';
import '../manager/user_manager.dart';

sealed class RegisterIntent {
  const RegisterIntent();
}

class RegisterUpdateAccount extends RegisterIntent {
  final String account;
  const RegisterUpdateAccount(this.account);
}

class RegisterUpdatePassword extends RegisterIntent {
  final String password;
  const RegisterUpdatePassword(this.password);
}

class RegisterUpdateConfirmPassword extends RegisterIntent {
  final String confirmPassword;
  const RegisterUpdateConfirmPassword(this.confirmPassword);
}

class RegisterSubmit extends RegisterIntent {
  const RegisterSubmit();
}

class RegisterGoLogin extends RegisterIntent {
  const RegisterGoLogin();
}

class RegisterState {
  final String account;
  final String password;
  final String confirmPassword;
  final bool isLoading;

  const RegisterState({
    required this.account,
    required this.password,
    required this.confirmPassword,
    required this.isLoading,
  });

  bool get canSubmit =>
      account.trim().isNotEmpty &&
      password.isNotEmpty &&
      confirmPassword.isNotEmpty &&
      password == confirmPassword &&
      !isLoading;

  RegisterState copyWith({
    String? account,
    String? password,
    String? confirmPassword,
    bool? isLoading,
  }) {
    return RegisterState(
      account: account ?? this.account,
      password: password ?? this.password,
      confirmPassword: confirmPassword ?? this.confirmPassword,
      isLoading: isLoading ?? this.isLoading,
    );
  }
}

sealed class RegisterEffect {
  const RegisterEffect();
}

class RegisterNavigateToMain extends RegisterEffect {
  const RegisterNavigateToMain();
}

class RegisterNavigateToLogin extends RegisterEffect {
  const RegisterNavigateToLogin();
}

class RegisterShowToast extends RegisterEffect {
  final String message;
  const RegisterShowToast(this.message);
}

class RegisterVm extends ChangeNotifier {
  RegisterVm({
    UserManager? userManager,
  }) : _userManager = userManager ?? UserManager.instance;

  final UserManager _userManager;
  final _effectController = StreamController<RegisterEffect>.broadcast();

  RegisterState _state = const RegisterState(
    account: '',
    password: '',
    confirmPassword: '',
    isLoading: false,
  );
  RegisterState get state => _state;
  Stream<RegisterEffect> get effects => _effectController.stream;

  void processIntent(RegisterIntent intent) {
    if (intent is RegisterUpdateAccount) {
      _state = _state.copyWith(account: intent.account);
      notifyListeners();
    } else if (intent is RegisterUpdatePassword) {
      _state = _state.copyWith(password: intent.password);
      notifyListeners();
    } else if (intent is RegisterUpdateConfirmPassword) {
      _state = _state.copyWith(confirmPassword: intent.confirmPassword);
      notifyListeners();
    } else if (intent is RegisterSubmit) {
      _submit();
    } else if (intent is RegisterGoLogin) {
      _effectController.add(const RegisterNavigateToLogin());
    }
  }

  Future<void> _submit() async {
    if (_state.account.isEmpty || _state.password.isEmpty || _state.confirmPassword.isEmpty) {
      _effectController.add(const RegisterShowToast('请填写完整信息'));
      return;
    }
    if (_state.password != _state.confirmPassword) {
      _effectController.add(const RegisterShowToast('两次密码输入不一致'));
      return;
    }
    _state = _state.copyWith(isLoading: true);
    notifyListeners();
    try {
      final auth = await _userManager.registerRemote(
        account: _state.account.trim(),
        password: _state.password,
        name: _state.account.trim(),
      );
      if ((auth.userId ?? 0) <= 0) {
        _effectController.add(const RegisterShowToast('注册失败'));
      } else {
        final session = UserSessionModel(
          userId: auth.userId ?? 0,
          account: auth.account ?? '',
          name: auth.name ?? '',
          avatarUrl: auth.avatarUrl ?? '',
          accessToken: auth.accessToken ?? '',
          password: '',
          isCurrent: true,
          lastLoginAt: DateTime.now().millisecondsSinceEpoch,
        );
        await _userManager.saveCurrentUser(session);
        AppSession.instance.updateUserId(session.userId);
        _effectController.add(const RegisterNavigateToMain());
      }
    } catch (e) {
      _effectController.add(RegisterShowToast('注册失败: $e'));
    } finally {
      _state = _state.copyWith(isLoading: false);
      notifyListeners();
    }
  }

  @override
  void dispose() {
    _effectController.close();
    super.dispose();
  }
}
