import 'package:dio/dio.dart';

import '../../constant/network_constant.dart';
import '../../network/safe_dio_log_interceptor.dart';
import '../../domain/dto/base_response.dart';
import '../../domain/dto/resp/oss_batch_delete_response.dart';
import '../../domain/dto/resp/oss_batch_upload_response.dart';
import '../../domain/dto/resp/oss_file_content_update_response.dart';
import '../../domain/dto/resp/oss_user_bucket_file_ids_response.dart';
import '../../domain/dto/resp/oss_user_bucket_file_item_list_response.dart';
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
      ) {
    if (dio == null) {
      _dio.interceptors.add(SafeDioLogInterceptor());
    }
  }

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

  Future<OssUserBucketFileItemListResponse> userBucketFileItemList({
    required String userId,
    required String bucketName,
  }) async {
    final headers = await _authHeaders();
    final resp = await _dio.post(
      '/oss/user/bucket/file/item/list',
      data: FormData.fromMap({
        'userId': userId,
        'bucketName': bucketName,
      }),
      options: Options(headers: headers),
    );
    final parsed = BaseResponse<OssUserBucketFileItemListResponse>.fromJson(
      resp.data as Map<String, dynamic>,
      (json) => OssUserBucketFileItemListResponse.fromJson(
        json as Map<String, dynamic>,
      ),
    );
    if (!parsed.isSuccess || parsed.data == null) {
      throw Exception(parsed.message ?? '查询文件明细失败');
    }
    return parsed.data!;
  }

  Future<OssBatchUploadResponse> batchUploadSingleFile({
    required String userId,
    String? bucketName,
    required String filePath,
    required String filename,
  }) async {
    final headers = await _authHeaders();
    final map = <String, dynamic>{
      'userId': userId,
      'files': await MultipartFile.fromFile(filePath, filename: filename),
    };
    if (bucketName != null && bucketName.isNotEmpty) {
      map['bucketName'] = bucketName;
    }
    final resp = await _dio.post(
      '/oss/upload/batch',
      data: FormData.fromMap(map),
      options: Options(headers: headers),
    );
    final parsed = BaseResponse<OssBatchUploadResponse>.fromJson(
      resp.data as Map<String, dynamic>,
      (json) => OssBatchUploadResponse.fromJson(json as Map<String, dynamic>),
    );
    if (!parsed.isSuccess || parsed.data == null) {
      throw Exception(parsed.message ?? '上传失败');
    }
    return parsed.data!;
  }

  Future<OssBatchDeleteResponse> batchDeleteFiles(List<String> fileIds) async {
    if (fileIds.isEmpty) {
      throw Exception('fileIds 为空');
    }
    final headers = await _authHeaders();
    final fd = FormData();
    for (final id in fileIds) {
      fd.fields.add(MapEntry('fileIdList', id));
    }
    final resp = await _dio.post(
      '/oss/file/delete/batch',
      data: fd,
      options: Options(headers: headers),
    );
    final parsed = BaseResponse<OssBatchDeleteResponse>.fromJson(
      resp.data as Map<String, dynamic>,
      (json) => OssBatchDeleteResponse.fromJson(json as Map<String, dynamic>),
    );
    if (!parsed.isSuccess || parsed.data == null) {
      throw Exception(parsed.message ?? '删除失败');
    }
    return parsed.data!;
  }

  Future<OssFileContentUpdateResponse> updateFileContent({
    required String fileId,
    required String filePath,
    required String filename,
  }) async {
    final headers = await _authHeaders();
    final resp = await _dio.post(
      '/oss/file/content/update',
      data: FormData.fromMap({
        'fileId': fileId,
        'file': await MultipartFile.fromFile(filePath, filename: filename),
      }),
      options: Options(headers: headers),
    );
    final parsed = BaseResponse<OssFileContentUpdateResponse>.fromJson(
      resp.data as Map<String, dynamic>,
      (json) =>
          OssFileContentUpdateResponse.fromJson(json as Map<String, dynamic>),
    );
    if (!parsed.isSuccess || parsed.data == null) {
      throw Exception(parsed.message ?? '更新文件失败');
    }
    return parsed.data!;
  }
}
