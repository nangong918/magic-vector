import 'package:dio/dio.dart';

import '../constant/network_constant.dart';
import 'auth_header_interceptor.dart';
import 'safe_dio_log_interceptor.dart';

/// 全局单例 Dio（拦截器顺序：鉴权头 → 日志）。
class AppDio {
  AppDio._();

  static Dio? _dio;

  static Dio get instance {
    _dio ??= () {
      final d = Dio(
        BaseOptions(
          baseUrl: NetworkConstant.baseUrl,
          connectTimeout:
              const Duration(milliseconds: NetworkConstant.connectTimeout),
          receiveTimeout:
              const Duration(milliseconds: NetworkConstant.receiveTimeout),
        ),
      );
      d.interceptors.add(AuthHeaderInterceptor());
      d.interceptors.add(SafeDioLogInterceptor());
      return d;
    }();
    return _dio!;
  }
}
