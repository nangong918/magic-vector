import 'package:dio/dio.dart';

import '../../constant/network_constant.dart';
import '../../domain/dto/base_response.dart';
import '../../domain/dto/resp/oss_user_bucket_file_ids_response.dart';
import '../../domain/dto/resp/oss_user_bucket_file_urls_response.dart';
import '../../domain/dto/resp/oss_user_bucket_list_response.dart';
import '../../manager/user_manager.dart';

/// v1.1 OSS 查询接口封装（需登录态：请求头 user_id / access_token）。
class OssRemoteApiSource {
  OssRemoteApiSource({Dio? dio}) : _dio = dio ?? Dio(
        BaseOptions(
          baseUrl: NetworkConstant.baseUrl,
          connectTimeout:
              const Duration(milliseconds: NetworkConstant.connectTimeout),
          receiveTimeout:
              const Duration(milliseconds: NetworkConstant.receiveTimeout),
        ),
      );

  final Dio _dio;

  Future<Map<String, String>> _authHeaders() async {
    final session = await UserManager.instance.getCurrentUser();
    if (session == null ||
        session.userId <= 0 ||
        session.accessToken.isEmpty) {
      throw Exception('未登录或 token 无效');
    }
    return {
      'user_id': session.userId.toString(),
      'access_token': session.accessToken,
    };
  }

  Future<OssUserBucketListResponse> userBucketList({required String userId}) async {
    final headers = await _authHeaders();
    final resp = await _dio.post(
      '/oss/user/bucket/list',
      data: FormData.fromMap({'userId': userId}),
      options: Options(headers: headers),
    );
    final parsed = BaseResponse<OssUserBucketListResponse>.fromJson(
      resp.data as Map<String, dynamic>,
      (json) => OssUserBucketListResponse.fromJson(json as Map<String, dynamic>),
    );
    if (!parsed.isSuccess || parsed.data == null) {
      throw Exception(parsed.message ?? '查询存储桶失败');
    }
    return parsed.data!;
  }

  Future<OssUserBucketFileIdsResponse> userBucketFileIdList({
    required String userId,
    required String bucketName,
  }) async {
    final headers = await _authHeaders();
    final resp = await _dio.post(
      '/oss/user/bucket/file/id/list',
      data: FormData.fromMap({
        'userId': userId,
        'bucketName': bucketName,
      }),
      options: Options(headers: headers),
    );
    final parsed = BaseResponse<OssUserBucketFileIdsResponse>.fromJson(
      resp.data as Map<String, dynamic>,
      (json) =>
          OssUserBucketFileIdsResponse.fromJson(json as Map<String, dynamic>),
    );
    if (!parsed.isSuccess || parsed.data == null) {
      throw Exception(parsed.message ?? '查询文件 id 失败');
    }
    return parsed.data!;
  }

  Future<OssUserBucketFileUrlsResponse> userBucketFileUrlList({
    required String userId,
    required String bucketName,
  }) async {
    final headers = await _authHeaders();
    final resp = await _dio.post(
      '/oss/user/bucket/file/url/list',
      data: FormData.fromMap({
        'userId': userId,
        'bucketName': bucketName,
      }),
      options: Options(headers: headers),
    );
    final parsed = BaseResponse<OssUserBucketFileUrlsResponse>.fromJson(
      resp.data as Map<String, dynamic>,
      (json) =>
          OssUserBucketFileUrlsResponse.fromJson(json as Map<String, dynamic>),
    );
    if (!parsed.isSuccess || parsed.data == null) {
      throw Exception(parsed.message ?? '查询文件 URL 失败');
    }
    return parsed.data!;
  }
}
