import 'package:dio/dio.dart';

import '../../domain/dto/resp/oss_batch_delete_response.dart';
import '../../domain/dto/resp/oss_batch_upload_response.dart';
import '../../domain/dto/resp/oss_file_content_update_response.dart';
import '../../domain/dto/resp/oss_user_bucket_file_item_list_response.dart';
import '../../domain/dto/resp/oss_user_bucket_list_response.dart';
import '../../network/api_request.dart';
import 'remote_request.dart';

/// 对齐 Android [RemoteApiSource] 中 OSS 相关接口：只返回解包后的 DTO。
class OssRemoteDataSource {
  OssRemoteDataSource(this._api);

  final ApiRequest _api;

  Future<OssUserBucketListResponse> ossUserBucketList(String userId) {
    return requestData(
      () => _api.ossUserBucketList(userId),
      emptyDataMessage: '存储桶列表响应为空',
    );
  }

  Future<OssUserBucketFileItemListResponse> ossUserBucketFileItemList({
    required String userId,
    required String bucketName,
  }) {
    return requestData(
      () => _api.ossUserBucketFileItemList(userId, bucketName),
      emptyDataMessage: '文件明细列表响应为空',
    );
  }

  Future<OssBatchUploadResponse> batchUploadSingleFile({
    required String userId,
    String? bucketName,
    required String filePath,
    required String filename,
  }) async {
    final file = await MultipartFile.fromFile(filePath, filename: filename);
    return requestData(
      () => _api.ossBatchUpload(userId, bucketName, file),
      emptyDataMessage: '上传响应为空',
    );
  }

  Future<OssBatchDeleteResponse> batchDeleteFiles(List<String> fileIds) async {
    if (fileIds.isEmpty) {
      throw ArgumentError('fileIds 为空');
    }
    final fd = FormData();
    for (final id in fileIds) {
      fd.fields.add(MapEntry('fileIdList', id));
    }
    return requestData(
      () => _api.ossBatchDelete(fd),
      emptyDataMessage: '删除响应为空',
    );
  }

  Future<OssFileContentUpdateResponse> updateFileContent({
    required String fileId,
    required String filePath,
    required String filename,
  }) async {
    final part = await MultipartFile.fromFile(filePath, filename: filename);
    return requestData(
      () => _api.ossUpdateFileContent(fileId, part),
      emptyDataMessage: '更新文件内容响应为空',
    );
  }
}
