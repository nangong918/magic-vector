import 'package:flutter/material.dart';

import '../data_source/remote/demo_remote_api_source.dart';
import '../network/app_api.dart';

class NetworkViewModel extends ChangeNotifier {
  NetworkViewModel({DemoRemoteApiSource? api})
      : _api = api ?? DemoRemoteApiSource(AppApi.instance);

  final DemoRemoteApiSource _api;

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
    try {
      final data = await _api.registerDemo(
        account: account,
        password: password,
        name: account,
      );
      _account = data.account;
      _loginToken = data.loginToken;
      _isLoggedIn = true;
      _statusColor = Colors.green;
      _statusMessage = _buildSuccessMessage('登录成功');
    } catch (_) {
      _setErrorMessage('网络错误，请稍后重试');
    }
    _setLoading(false);
  }

  Future<void> refreshToken({
    required String account,
  }) async {
    if (_isLoading) return;
    _setLoading(true);
    try {
      final data = await _api.resetTokenDemo(account);
      _account = data.account.isNotEmpty ? data.account : _account;
      _loginToken =
          data.loginToken.isNotEmpty ? data.loginToken : _loginToken;
      _statusColor = Colors.green;
      _statusMessage = _buildSuccessMessage('Token已刷新');
    } catch (_) {
      _setErrorMessage('网络错误，请稍后重试');
    }
    _setLoading(false);
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
