import '../../domain/dto/req/user_test_req.dart';
import '../../domain/model/user_test_demo_model.dart';
import '../../network/api_request.dart';
import 'remote_request.dart';

/// Demo `/test/network/*`；与正式用户域 [UserRemoteApiSource] 分离。
class DemoRemoteApiSource {
  DemoRemoteApiSource(this._api);

  final ApiRequest _api;

  Future<UserTestDemoModel> registerDemo({
    required String account,
    required String password,
    required String name,
  }) async {
    final data = await requestData(
      () => _api.registerTest(
        UserTestReq(account: account, password: password, name: name),
      ),
      emptyDataMessage: '注册响应为空',
    );
    return UserTestDemoModel(
      account: data.account ?? '',
      loginToken: data.loginToken ?? '',
    );
  }

  Future<UserTestDemoModel> resetTokenDemo(String account) async {
    final data = await requestData(
      () => _api.resetToken(account),
      emptyDataMessage: '刷新 Token 响应为空',
    );
    return UserTestDemoModel(
      account: data.account ?? '',
      loginToken: data.loginToken ?? '',
    );
  }
}
