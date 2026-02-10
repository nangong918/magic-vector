import 'dart:convert';

import 'package:dio/dio.dart';

import '../constant/NetworkConstant.dart';
import '../domain/vo/BaseResponse.dart';
import '../domain/vo/UserTestReq.dart';
import '../domain/vo/UserTestResp.dart';
import 'ApiRequest.dart';
import 'BaseApiRequestImpl.dart';

class ApiRequestImpl extends BaseApiRequestImpl implements ApiRequest {
  ApiRequestImpl({Dio? dio})
      : _dio = dio ??
            Dio(
              BaseOptions(
                baseUrl: NetworkConstant.baseUrl,
                connectTimeout: 10000,
                receiveTimeout: 10000,
              ),
            );

  final Dio _dio;

  @override
  Future<BaseResponse<UserTestResp>> register(UserTestReq req) async {
    final response = await _dio.post(
      '/test/network/register',
      data: req.toJson(),
    );
    return _parseUserResponse(response);
  }

  @override
  Future<BaseResponse<UserTestResp>> resetToken(String account) async {
    final response = await _dio.get(
      '/test/network/resetToken',
      queryParameters: {'account': account},
    );
    return _parseUserResponse(response);
  }

  Future<void> registerWithCallback(
    UserTestReq req,
    OnSuccessCallback<BaseResponse<UserTestResp>>? successCallback,
    OnThrowableCallback? throwableCallback,
  ) {
    return sendRequestCallback(
      apiCall: () => register(req),
      successCallback: successCallback,
      throwableCallback: throwableCallback,
    );
  }

  Future<void> resetTokenWithCallback(
    String account,
    OnSuccessCallback<BaseResponse<UserTestResp>>? successCallback,
    OnThrowableCallback? throwableCallback,
  ) {
    return sendRequestCallback(
      apiCall: () => resetToken(account),
      successCallback: successCallback,
      throwableCallback: throwableCallback,
    );
  }

  BaseResponse<UserTestResp> _parseUserResponse(Response<dynamic> response) {
    final raw = _normalizeMap(response.data);
    return BaseResponse.fromJson(raw, (json) => UserTestResp.fromJson(json));
  }

  Map<String, dynamic> _normalizeMap(dynamic data) {
    if (data is Map<String, dynamic>) {
      return data;
    }
    if (data is Map) {
      return Map<String, dynamic>.from(data);
    }
    if (data is String && data.isNotEmpty) {
      return jsonDecode(data) as Map<String, dynamic>;
    }
    return <String, dynamic>{};
  }
}
