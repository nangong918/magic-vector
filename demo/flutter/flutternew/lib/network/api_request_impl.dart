import 'package:dio/dio.dart';

import '../constant/network_constant.dart';
import '../domain/dto/base_response.dart';
import '../domain/dto/req/user_test_req.dart';
import '../domain/dto/resp/user_test_resp.dart';
import 'api_request.dart';
import 'base_api_request_impl.dart';

class ApiRequestImpl extends BaseApiRequestImpl {
  final Dio _dio;

  late final ApiRequest _api;

  ApiRequestImpl({Dio? dio})
      : _dio = dio ??
            Dio(
              BaseOptions(
                connectTimeout:
                    Duration(milliseconds: NetworkConstant.connectTimeout),
                receiveTimeout:
                    Duration(milliseconds: NetworkConstant.receiveTimeout),
              ),
            ) {
    _api = ApiRequest(_dio, baseUrl: NetworkConstant.baseUrl);
  }

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
