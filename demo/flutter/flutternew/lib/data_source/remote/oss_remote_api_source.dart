import 'package:dio/dio.dart';

import '../../domain/convertor/oss_convertor.dart';
import '../../domain/model/oss/oss_batch_upload_model.dart';
import '../../domain/model/oss/oss_bucket_file_item_model.dart';
import '../../domain/model/oss/oss_file_content_update_model.dart';
import '../../domain/model/oss/oss_user_bucket_list_model.dart';
import '../../network/api_request.dart';
import 'remote_request.dart';

/// 对齐 Android [OssRemoteApiSource]：仅返回业务 Model。
class OssRemoteApiSource {
  OssRemoteApiSource(this._api);

  final ApiRequest _api;

  Future<OssUserBucketListModel> ossUserBucketList(String userId) async {
    final dto = await requestData(
      () => _api.ossUserBucketList(userId),
      emptyDataMessage: '存储桶列表响应为空',
    );
    return OssConvertor.userBucketList(dto);
  }

  Future<List<OssBucketFileItemModel>> ossUserBucketFileItemList({
    required String userId,
    required String bucketName,
  }) async {
    final dto = await requestData(
      () => _api.ossUserBucketFileItemList(userId, bucketName),
      emptyDataMessage: '文件明细列表响应为空',
    );
    return OssConvertor.fileItemList(dto);
  }

  Future<OssBatchUploadModel> batchUploadSingleFile({
    required String userId,
    String? bucketName,
    required String filePath,
    required String filename,
  }) async {
    final file = await MultipartFile.fromFile(filePath, filename: filename);
    final dto = await requestData(
      () => _api.ossBatchUpload(userId, bucketName, file),
      emptyDataMessage: '上传响应为空',
    );
    return OssConvertor.batchUpload(dto);
  }

  Future<void> batchDeleteFiles(List<String> fileIds) async {
    if (fileIds.isEmpty) {
      throw ArgumentError('fileIds 为空');
    }
    final fd = FormData();
    for (final id in fileIds) {
      fd.fields.add(MapEntry('fileIdList', id));
    }
    await requestData(
      () => _api.ossBatchDelete(fd),
      emptyDataMessage: '删除响应为空',
    );
  }

  Future<OssFileContentUpdateModel> updateFileContent({
    required String fileId,
    required String filePath,
    required String filename,
  }) async {
    final part = await MultipartFile.fromFile(filePath, filename: filename);
    final dto = await requestData(
      () => _api.ossUpdateFileContent(fileId, part),
      emptyDataMessage: '更新文件内容响应为空',
    );
    return OssConvertor.fileContentUpdate(dto);
  }
}
