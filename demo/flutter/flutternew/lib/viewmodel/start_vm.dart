import 'dart:async';

import 'package:flutter/foundation.dart';

import '../constant/auth_constant.dart';
import '../manager/app_session.dart';
import '../manager/user_manager.dart';

sealed class StartIntent {
  const StartIntent();
}

class StartInitialize extends StartIntent {
  const StartInitialize();
}

class StartState {
  final bool isLoading;
  const StartState({required this.isLoading});

  StartState copyWith({bool? isLoading}) {
    return StartState(isLoading: isLoading ?? this.isLoading);
  }
}

sealed class StartEffect {
  const StartEffect();
}

class StartNavigateToMain extends StartEffect {
  const StartNavigateToMain();
}

class StartNavigateToLogin extends StartEffect {
  const StartNavigateToLogin();
}

class StartShowToast extends StartEffect {
  final String message;
  const StartShowToast(this.message);
}

class StartVm extends ChangeNotifier {
  StartVm({
    UserManager? userManager,
  }) : _userManager = userManager ?? UserManager.instance;

  final UserManager _userManager;
  final _effectController = StreamController<StartEffect>.broadcast();

  Stream<StartEffect> get effects => _effectController.stream;
  StartState _state = const StartState(isLoading: true);
  StartState get state => _state;

  void processIntent(StartIntent intent) {
    if (intent is StartInitialize) {
      _initialize();
    }
  }

  Future<void> _initialize() async {
    _state = _state.copyWith(isLoading: true);
    notifyListeners();
    final begin = DateTime.now().millisecondsSinceEpoch;
    final effect = await _resolveTarget();
    final elapsed = DateTime.now().millisecondsSinceEpoch - begin;
    final delay = AuthConstant.startDelayMs - elapsed;
    if (delay > 0) {
      await Future<void>.delayed(Duration(milliseconds: delay));
    }
    _state = _state.copyWith(isLoading: false);
    notifyListeners();
    _effectController.add(effect);
  }

  Future<StartEffect> _resolveTarget() async {
    final local = await _userManager.getCurrentUser();
    if (local == null || local.accessToken.isEmpty) {
      await _userManager.clearCurrentUser();
      AppSession.instance.clearUserId();
      return const StartNavigateToLogin();
    }
    try {
      final verify = await _userManager.verifyAccessTokenRemote(
        userId: local.userId,
        accessToken: local.accessToken,
      );
      if (verify) {
        AppSession.instance.updateUserId(local.userId);
        return const StartNavigateToMain();
      }
      await _userManager.clearCurrentUser();
      AppSession.instance.clearUserId();
      return const StartNavigateToLogin();
    } catch (_) {
      _effectController.add(const StartShowToast('服务器验证失败，请稍后重试'));
      await _userManager.clearCurrentUser();
      AppSession.instance.clearUserId();
      return const StartNavigateToLogin();
    }
  }

  @override
  void dispose() {
    _effectController.close();
    super.dispose();
  }
}
