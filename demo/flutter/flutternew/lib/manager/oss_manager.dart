import '../data_source/remote/oss_remote_api_source.dart';
import '../domain/model/oss/oss_batch_upload_model.dart';
import '../domain/model/oss/oss_bucket_file_item_model.dart';
import '../domain/model/oss/oss_file_content_update_model.dart';
import '../domain/model/oss/oss_user_bucket_list_model.dart';
import '../network/app_api.dart';

/// 对齐 Android [OssManager]：持有 [OssRemoteApiSource]，向上仅返回 Model。
class OssManager {
  OssManager._();

  static final OssManager instance = OssManager._();

  OssRemoteApiSource? _remote;

  OssRemoteApiSource get _client =>
      _remote ??= OssRemoteApiSource(AppApi.instance);

  Future<OssUserBucketListModel> syncUserBucketList(String userId) =>
      _client.ossUserBucketList(userId);

  Future<List<OssBucketFileItemModel>> syncBucketFileItemList({
    required String userId,
    required String bucketName,
  }) =>
      _client.ossUserBucketFileItemList(userId: userId, bucketName: bucketName);

  Future<OssBatchUploadModel> batchUploadSingle({
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

  Future<void> batchDeleteFiles(List<String> fileIds) =>
      _client.batchDeleteFiles(fileIds);

  Future<OssFileContentUpdateModel> updateFileContent({
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
