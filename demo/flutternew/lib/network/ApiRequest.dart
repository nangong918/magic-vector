import '../domain/dto/BaseResponse.dart';
import '../domain/dto/req/UserTestReq.dart';
import '../domain/dto/resp/UserTestResp.dart';

abstract class ApiRequest {
  Future<BaseResponse<UserTestResp>> register(UserTestReq req);

  Future<BaseResponse<UserTestResp>> resetToken(String account);
}
