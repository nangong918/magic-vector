import 'package:dio/dio.dart';

import '../manager/user_manager.dart';

/// 登录、注册不加头；其余请求附加 `user_id` / `access_token`。
class AuthHeaderInterceptor extends Interceptor {
  static const _noAuthPathSuffixes = <String>[
    '/user/login',
    '/user/register',
  ];

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    final path = options.uri.path;
    if (_noAuthPathSuffixes.any((s) => path.endsWith(s))) {
      handler.next(options);
      return;
    }
    final session = UserManager.instance.currentSessionSync;
    if (session != null &&
        session.userId > 0 &&
        session.accessToken.isNotEmpty) {
      options.headers['user_id'] = session.userId.toString();
      options.headers['access_token'] = session.accessToken;
    }
    handler.next(options);
  }
}
