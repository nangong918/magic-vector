import 'package:dio/dio.dart';

import '../../constant/NetworkConstant.dart';
import '../../domain/dto/BaseResponse.dart';
import '../../domain/dto/auth/user_auth_response.dart';
import '../../domain/dto/auth/user_login_request.dart';
import '../../domain/dto/auth/user_token_verify_request.dart';
import '../../domain/dto/auth/user_token_verify_response.dart';

class AuthRemoteApiSource {
  AuthRemoteApiSource({Dio? dio}) : _dio = dio ?? Dio(
    BaseOptions(
      baseUrl: NetworkConstant.baseUrl,
      connectTimeout: const Duration(milliseconds: NetworkConstant.connectTimeout),
      receiveTimeout: const Duration(milliseconds: NetworkConstant.receiveTimeout),
    ),
  );

  final Dio _dio;

  Future<UserAuthResponse> login(UserLoginRequest request) async {
    final resp = await _dio.post('/user/login', data: request.toJson());
    final parsed = BaseResponse<UserAuthResponse>.fromJson(
      resp.data as Map<String, dynamic>,
      (json) => UserAuthResponse.fromJson(json as Map<String, dynamic>),
    );
    if (!parsed.isSuccess || parsed.data == null) {
      throw Exception(parsed.message ?? '登录失败');
    }
    return parsed.data!;
  }

  Future<UserAuthResponse> register({
    required String account,
    required String password,
    required String name,
  }) async {
    final form = FormData.fromMap({
      'account': account,
      'password': password,
      'name': name,
    });
    final resp = await _dio.post('/user/register', data: form);
    final parsed = BaseResponse<UserAuthResponse>.fromJson(
      resp.data as Map<String, dynamic>,
      (json) => UserAuthResponse.fromJson(json as Map<String, dynamic>),
    );
    if (!parsed.isSuccess || parsed.data == null) {
      throw Exception(parsed.message ?? '注册失败');
    }
    return parsed.data!;
  }

  Future<UserTokenVerifyResponse> verifyAccessToken(
    UserTokenVerifyRequest request,
  ) async {
    final resp = await _dio.post('/user/token/verify', data: request.toJson());
    final parsed = BaseResponse<UserTokenVerifyResponse>.fromJson(
      resp.data as Map<String, dynamic>,
      (json) => UserTokenVerifyResponse.fromJson(json as Map<String, dynamic>),
    );
    if (!parsed.isSuccess || parsed.data == null) {
      throw Exception(parsed.message ?? 'Token校验失败');
    }
    return parsed.data!;
  }
}
