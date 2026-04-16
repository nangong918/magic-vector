import '../../domain/dto/req/user_login_request.dart';
import '../../domain/dto/req/user_token_verify_request.dart';
import '../../domain/dto/resp/user_auth_response.dart';
import '../../domain/dto/resp/user_token_verify_response.dart';
import '../../network/api_request.dart';
import 'remote_request.dart';

/// 对齐 Android [RemoteApiSource] 中用户相关接口：只返回解包后的 DTO，不暴露 [BaseResponse]。
class UserRemoteDataSource {
  UserRemoteDataSource(this._api);

  final ApiRequest _api;

  Future<UserAuthResponse> login(UserLoginRequest request) {
    return requestData(
      () => _api.userLogin(request),
      emptyDataMessage: '登录响应为空',
    );
  }

  Future<UserAuthResponse> register({
    required String account,
    required String password,
    required String name,
  }) {
    return requestData(
      () => _api.userRegister(account, password, name),
      emptyDataMessage: '注册响应为空',
    );
  }

  Future<UserTokenVerifyResponse> verifyAccessToken(
    UserTokenVerifyRequest request,
  ) {
    return requestData(
      () => _api.userVerifyAccessToken(request),
      emptyDataMessage: 'Token验证响应为空',
    );
  }
}
