import '../../domain/convertor/user_convertor.dart';
import '../../domain/dto/req/user_login_request.dart';
import '../../domain/dto/req/user_token_verify_request.dart';
import '../../domain/model/user_session_model.dart';
import '../../network/api_request.dart';
import 'remote_request.dart';

/// 对齐 Android [UserRemoteApiSource]：不对外暴露 Request/Response，仅 [UserSessionModel] / 基本类型。
class UserRemoteApiSource {
  UserRemoteApiSource(this._api);

  final ApiRequest _api;

  Future<UserSessionModel> login({
    required String account,
    required String password,
  }) async {
    final auth = await requestData(
      () => _api.userLogin(
        UserLoginRequest(account: account, password: password),
      ),
      emptyDataMessage: '登录响应为空',
    );
    return UserConvertor.authResponseToSessionModel(auth, password);
  }

  Future<UserSessionModel> register({
    required String account,
    required String password,
    required String name,
  }) async {
    final auth = await requestData(
      () => _api.userRegister(account, password, name),
      emptyDataMessage: '注册响应为空',
    );
    return UserConvertor.authResponseToSessionModel(auth, password);
  }

  Future<bool> verifyAccessToken({
    required int userId,
    required String accessToken,
  }) async {
    final verify = await requestData(
      () => _api.userVerifyAccessToken(
        UserTokenVerifyRequest(userId: userId, accessToken: accessToken),
      ),
      emptyDataMessage: 'Token验证响应为空',
    );
    return verify.valid ?? false;
  }
}
