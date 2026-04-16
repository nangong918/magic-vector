import '../constant/network_constant.dart';
import 'api_request.dart';
import 'app_dio.dart';

/// 全局 [ApiRequest]（Retrofit），与 Android [MainApplication] 中单例 Api 对齐。
class AppApi {
  AppApi._();

  static ApiRequest? _request;

  static ApiRequest get instance {
    return _request ??= ApiRequest(
      AppDio.instance,
      baseUrl: NetworkConstant.baseUrl,
    );
  }

  /// 测试注入。
  static void resetForTest([ApiRequest? api]) {
    _request = api;
  }
}
