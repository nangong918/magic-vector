import 'dart:async';

import 'package:flutter/foundation.dart';

import '../data/remote/auth_remote_api_source.dart';
import '../domain/dto/req/user_login_request.dart';
import '../domain/model/user_session_model.dart';
import '../manager/app_session.dart';
import '../manager/user_manager.dart';

sealed class LoginIntent {
  const LoginIntent();
}

class LoginUpdateAccount extends LoginIntent {
  final String account;
  const LoginUpdateAccount(this.account);
}

class LoginUpdatePassword extends LoginIntent {
  final String password;
  const LoginUpdatePassword(this.password);
}

class LoginSubmit extends LoginIntent {
  const LoginSubmit();
}

class LoginGoRegister extends LoginIntent {
  const LoginGoRegister();
}

class LoginSelectSavedAccount extends LoginIntent {
  final String account;
  const LoginSelectSavedAccount(this.account);
}

class LoginTouristAccess extends LoginIntent {
  const LoginTouristAccess();
}

class LoginState {
  final String account;
  final String password;
  final bool isLoading;
  final List<UserSessionModel> savedSessions;

  const LoginState({
    required this.account,
    required this.password,
    required this.isLoading,
    required this.savedSessions,
  });

  bool get canSubmit => account.trim().isNotEmpty && password.isNotEmpty && !isLoading;

  LoginState copyWith({
    String? account,
    String? password,
    bool? isLoading,
    List<UserSessionModel>? savedSessions,
  }) {
    return LoginState(
      account: account ?? this.account,
      password: password ?? this.password,
      isLoading: isLoading ?? this.isLoading,
      savedSessions: savedSessions ?? this.savedSessions,
    );
  }
}

sealed class LoginEffect {
  const LoginEffect();
}

class LoginNavigateToMain extends LoginEffect {
  const LoginNavigateToMain();
}

class LoginNavigateToRegister extends LoginEffect {
  const LoginNavigateToRegister();
}

class LoginShowToast extends LoginEffect {
  final String message;
  const LoginShowToast(this.message);
}

class LoginVm extends ChangeNotifier {
  LoginVm({
    AuthRemoteApiSource? remoteApiSource,
    UserManager? userManager,
  }) : _remoteApiSource = remoteApiSource ?? AuthRemoteApiSource(),
       _userManager = userManager ?? UserManager.instance {
    _loadSavedAccounts();
  }

  final AuthRemoteApiSource _remoteApiSource;
  final UserManager _userManager;
  final _effectController = StreamController<LoginEffect>.broadcast();

  LoginState _state = const LoginState(
    account: '',
    password: '',
    isLoading: false,
    savedSessions: [],
  );
  LoginState get state => _state;
  Stream<LoginEffect> get effects => _effectController.stream;

  void processIntent(LoginIntent intent) {
    if (intent is LoginUpdateAccount) {
      _state = _state.copyWith(account: intent.account);
      notifyListeners();
    } else if (intent is LoginUpdatePassword) {
      _state = _state.copyWith(password: intent.password);
      notifyListeners();
    } else if (intent is LoginSelectSavedAccount) {
      _selectSavedAccount(intent.account);
    } else if (intent is LoginSubmit) {
      _submit();
    } else if (intent is LoginGoRegister) {
      _effectController.add(const LoginNavigateToRegister());
    } else if (intent is LoginTouristAccess) {
      _touristAccess();
    }
  }

  Future<void> _loadSavedAccounts() async {
    final list = (await _userManager.getAllUsers())
        .where((e) => !(e.accessToken == 'tourist' || e.account == 'tourist' || e.userId == 1))
        .toList();
    _state = _state.copyWith(savedSessions: list);
    if (list.isNotEmpty) {
      _state = _state.copyWith(
        account: _state.account.isEmpty ? list.first.account : _state.account,
        password: _state.password.isEmpty ? list.first.password : _state.password,
      );
    }
    notifyListeners();
  }

  void _selectSavedAccount(String account) {
    final hit = _state.savedSessions.where((e) => e.account == account).toList();
    if (hit.isEmpty) return;
    final selected = hit.first;
    _state = _state.copyWith(account: selected.account, password: selected.password);
    notifyListeners();
  }

  Future<void> _submit() async {
    if (!_state.canSubmit) {
      _effectController.add(const LoginShowToast('请输入账号和密码'));
      return;
    }
    _state = _state.copyWith(isLoading: true);
    notifyListeners();
    try {
      final auth = await _remoteApiSource.login(
        UserLoginRequest(
          account: _state.account.trim(),
          password: _state.password,
        ),
      );
      if ((auth.userId ?? 0) <= 0) {
        _effectController.add(const LoginShowToast('登录失败'));
      } else {
        final session = UserSessionModel(
          userId: auth.userId ?? 0,
          account: auth.account ?? '',
          name: auth.name ?? '',
          avatarUrl: auth.avatarUrl ?? '',
          accessToken: auth.accessToken ?? '',
          password: _state.password,
          isCurrent: true,
          lastLoginAt: DateTime.now().millisecondsSinceEpoch,
        );
        await _userManager.saveCurrentUser(session);
        AppSession.instance.updateUserId(session.userId);
        _effectController.add(const LoginNavigateToMain());
      }
    } catch (e) {
      _effectController.add(LoginShowToast('登录失败: $e'));
    } finally {
      _state = _state.copyWith(isLoading: false);
      notifyListeners();
      _loadSavedAccounts();
    }
  }

  Future<void> _touristAccess() async {
    final session = UserSessionModel(
      userId: 1,
      account: 'tourist',
      name: '游客',
      avatarUrl: '',
      accessToken: 'tourist',
      password: '',
      isCurrent: true,
      lastLoginAt: DateTime.now().millisecondsSinceEpoch,
    );
    await _userManager.saveCurrentUser(session);
    AppSession.instance.updateUserId(session.userId);
    _effectController.add(const LoginNavigateToMain());
  }

  @override
  void dispose() {
    _effectController.close();
    super.dispose();
  }
}
