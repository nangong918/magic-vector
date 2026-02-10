import 'package:flutter/material.dart';

import '../domain/vo/BaseResponse.dart';
import '../domain/vo/UserTestReq.dart';
import '../domain/vo/UserTestResp.dart';
import '../network/ApiRequestImpl.dart';

class NetworkViewModel extends ChangeNotifier {
  NetworkViewModel({ApiRequestImpl? api}) : _api = api ?? ApiRequestImpl();

  final ApiRequestImpl _api;

  bool _isLoggedIn = false;
  bool _isLoading = false;
  String _account = '';
  String _loginToken = '';
  String _statusMessage = '请登录';
  Color _statusColor = Colors.red;

  bool get isLoggedIn => _isLoggedIn;
  bool get isLoading => _isLoading;
  String get account => _account;
  String get loginToken => _loginToken;
  String get statusMessage => _statusMessage;
  Color get statusColor => _statusColor;

  Future<void> login({
    required String account,
    required String password,
  }) async {
    if (_isLoading) return;
    _setLoading(true);

    final req = UserTestReq(
      account: account,
      password: password,
      name: account,
    );

    await _api.registerWithCallback(
      req,
      _handleLoginSuccess,
      _handleThrowable,
    );

    _setLoading(false);
  }

  Future<void> refreshToken({
    required String account,
  }) async {
    if (_isLoading) return;
    _setLoading(true);

    await _api.resetTokenWithCallback(
      account,
      _handleRefreshSuccess,
      _handleThrowable,
    );

    _setLoading(false);
  }

  void _handleLoginSuccess(BaseResponse<UserTestResp> response) {
    if (response.isSuccess && response.data != null) {
      _account = response.data?.account ?? '';
      _loginToken = response.data?.loginToken ?? '';
      _isLoggedIn = true;
      _statusColor = Colors.green;
      _statusMessage = _buildSuccessMessage('登录成功');
    } else {
      _setErrorMessage(response.message ?? '登录失败');
    }
    notifyListeners();
  }

  void _handleRefreshSuccess(BaseResponse<UserTestResp> response) {
    if (response.isSuccess && response.data != null) {
      _account = response.data?.account ?? _account;
      _loginToken = response.data?.loginToken ?? _loginToken;
      _statusColor = Colors.green;
      _statusMessage = _buildSuccessMessage('Token已刷新');
    } else {
      _setErrorMessage(response.message ?? '刷新失败');
    }
    notifyListeners();
  }

  void _handleThrowable(Object error, StackTrace stackTrace) {
    _setErrorMessage('网络错误，请稍后重试');
    notifyListeners();
  }

  void _setLoading(bool loading) {
    _isLoading = loading;
    notifyListeners();
  }

  void _setErrorMessage(String message) {
    _statusColor = Colors.red;
    _statusMessage = message;
  }

  String _buildSuccessMessage(String title) {
    final buffer = StringBuffer(title);
    if (_account.isNotEmpty) {
      buffer.write('\n账号: $_account');
    }
    if (_loginToken.isNotEmpty) {
      buffer.write('\nToken: $_loginToken');
    }
    return buffer.toString();
  }
}
