import '../data/remote/oss_remote_data_source.dart';
import '../domain/convertor/oss_convertor.dart';
import '../domain/dto/resp/oss_batch_upload_response.dart';
import '../domain/dto/resp/oss_file_content_update_response.dart';
import '../domain/model/oss/oss_bucket_file_item_model.dart';
import '../domain/model/oss/oss_user_bucket_list_model.dart';
import '../network/app_api.dart';

/// 对齐 Android [OssManager]：持有 [OssRemoteDataSource]，向上返回 Model（上传/更新等可暂用 DTO）。
class OssManager {
  OssManager._();

  static final OssManager instance = OssManager._();

  OssRemoteDataSource? _remote;

  OssRemoteDataSource get _client =>
      _remote ??= OssRemoteDataSource(AppApi.instance);

  Future<OssUserBucketListModel> syncUserBucketList(String userId) async {
    final dto = await _client.ossUserBucketList(userId);
    return OssConvertor.userBucketList(dto);
  }

  Future<List<OssBucketFileItemModel>> syncBucketFileItemList({
    required String userId,
    required String bucketName,
  }) async {
    final dto = await _client.ossUserBucketFileItemList(
      userId: userId,
      bucketName: bucketName,
    );
    return OssConvertor.fileItemList(dto);
  }

  Future<OssBatchUploadResponse> batchUploadSingle({
    required String userId,
    String? bucketName,
    required String filePath,
    required String filename,
  }) {
    return _client.batchUploadSingleFile(
      userId: userId,
      bucketName: bucketName,
      filePath: filePath,
      filename: filename,
    );
  }

  Future<void> batchDeleteFiles(List<String> fileIds) async {
    await _client.batchDeleteFiles(fileIds);
  }

  Future<OssFileContentUpdateResponse> updateFileContent({
    required String fileId,
    required String filePath,
    required String filename,
  }) {
    return _client.updateFileContent(
      fileId: fileId,
      filePath: filePath,
      filename: filename,
    );
  }
}
