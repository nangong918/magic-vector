import '../domain/vo/BaseResponse.dart';
import '../domain/vo/UserTestReq.dart';
import '../domain/vo/UserTestResp.dart';

abstract class ApiRequest {
  Future<BaseResponse<UserTestResp>> register(UserTestReq req);

  Future<BaseResponse<UserTestResp>> resetToken(String account);
}
