import '../dto/resp/oss_batch_upload_response.dart';
import '../dto/resp/oss_file_content_update_response.dart';
import '../dto/resp/oss_user_bucket_file_item_list_response.dart';
import '../dto/resp/oss_user_bucket_file_item_row.dart';
import '../dto/resp/oss_user_bucket_list_response.dart';
import '../dto/resp/oss_upload_item_result.dart';
import '../model/oss/oss_batch_upload_model.dart';
import '../model/oss/oss_bucket_file_item_model.dart';
import '../model/oss/oss_file_content_update_model.dart';
import '../model/oss/oss_upload_item_model.dart';
import '../model/oss/oss_user_bucket_list_model.dart';

class OssConvertor {
  OssConvertor._();

  static int parseUserIdWire(String? s) => int.tryParse(s ?? '') ?? 0;

  static int parseFileIdWire(String? s) => int.tryParse(s ?? '') ?? 0;

  static OssUserBucketListModel userBucketList(OssUserBucketListResponse r) {
    return OssUserBucketListModel(
      userId: parseUserIdWire(r.userId),
      bucketNames: List<String>.from(r.bucketNameList ?? const []),
    );
  }

  static OssBucketFileItemModel fileItem(OssUserBucketFileItemRow r) {
    return OssBucketFileItemModel(
      fileId: parseFileIdWire(r.fileId),
      originFileName: r.originFileName ?? '',
      url: r.url ?? '',
    );
  }

  static List<OssBucketFileItemModel> fileItemList(
    OssUserBucketFileItemListResponse r,
  ) {
    return (r.items ?? const <OssUserBucketFileItemRow>[])
        .map(fileItem)
        .toList();
  }

  static OssUploadItemModel uploadItem(OssUploadItemResult r) {
    return OssUploadItemModel(
      originFileName: r.originFileName ?? '',
      success: r.success,
      duplicated: r.duplicated,
      fileId: (r.fileId ?? 0).toString(),
      url: r.url ?? '',
      message: r.message ?? '',
    );
  }

  static OssBatchUploadModel batchUpload(OssBatchUploadResponse r) {
    final items = (r.items ?? const <OssUploadItemResult>[])
        .map(uploadItem)
        .toList();
    return OssBatchUploadModel(
      userId: r.userId ?? 0,
      bucketName: r.bucketName ?? '',
      successCount: r.successCount ?? 0,
      failCount: r.failCount ?? 0,
      items: items,
    );
  }

  static OssFileContentUpdateModel fileContentUpdate(
    OssFileContentUpdateResponse r,
  ) {
    return OssFileContentUpdateModel(
      fileId: (r.fileId ?? 0).toString(),
      originFileName: r.originFileName ?? '',
      url: r.url ?? '',
      updated: r.updated ?? false,
      message: r.message ?? '',
    );
  }
}
