import 'package:dio/dio.dart';

import '../constant/NetworkConstant.dart';
import '../domain/dto/BaseResponse.dart';
import '../domain/dto/req/UserTestReq.dart';
import '../domain/dto/resp/UserTestResp.dart';
import 'ApiRequest.dart';
import 'BaseApiRequestImpl.dart';

class ApiRequestImpl extends BaseApiRequestImpl {
  final Dio _dio;

  late final ApiRequest _api; // 延迟初始化

  ApiRequestImpl({Dio? dio})
      : _dio = dio ?? Dio(
    BaseOptions(
      connectTimeout: Duration(milliseconds: NetworkConstant.connectTimeout),
      receiveTimeout: Duration(milliseconds: NetworkConstant.receiveTimeout),
    ),
  ) {
    _api = ApiRequest(_dio, baseUrl: NetworkConstant.baseUrl);
  }

  //  // POST 请求：注册接口
  //   @POST('/test/network/register')
  //   Future<BaseResponse<UserTestResp>> register(@Body() UserTestReq req);
  Future<void> register(
    UserTestReq req,
    OnSuccessCallback<BaseResponse<UserTestResp>>? successCallback,
    OnThrowableCallback? throwableCallback,
  ) {
    return sendRequestCallback(
      apiCall: () => _api.register(req),
      successCallback: successCallback,
      throwableCallback: throwableCallback,
    );
  }

  //  // GET 请求：重置 Token 接口
  //   @GET('/test/network/resetToken')
  //   Future<BaseResponse<UserTestResp>> resetToken(@Query('account') String account);
  Future<void> resetToken(
    String account,
    OnSuccessCallback<BaseResponse<UserTestResp>>? successCallback,
    OnThrowableCallback? throwableCallback,
  ) {
    return sendRequestCallback(
      apiCall: () => _api.resetToken(account),
      successCallback: successCallback,
      throwableCallback: throwableCallback,
    );
  }
}
