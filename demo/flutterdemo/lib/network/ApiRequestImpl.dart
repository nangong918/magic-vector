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

  // 辅助方法：将数据转换为标准 Map<String, dynamic>; dynamic相当于Java的Object
  Map<String, dynamic> _normalizeMap(dynamic data) {
    // 情况1：如果已经是标准的 Map<String, dynamic>，直接返回（最优路径）
    if (data is Map<String, dynamic>) {
      return data;
    }
    // 情况2：如果是非泛型 Map（比如 Map<dynamic, dynamic>），转换成标准泛型 Map
    if (data is Map) {
      return Map<String, dynamic>.from(data);
    }
    // 情况3：如果是非空字符串，先 JSON 解码再转 Map
    if (data is String && data.isNotEmpty) {
      return jsonDecode(data) as Map<String, dynamic>;
    }
    // 情况4：以上都不满足（比如 null/空字符串/数字等），返回空 Map 避免后续解析报错
    return <String, dynamic>{};
  }
}
