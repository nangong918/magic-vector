import 'oss_upload_item_model.dart';

class OssBatchUploadModel {
  final int userId;
  final String bucketName;
  final int successCount;
  final int failCount;
  final List<OssUploadItemModel> items;

  const OssBatchUploadModel({
    required this.userId,
    required this.bucketName,
    required this.successCount,
    required this.failCount,
    required this.items,
  });
}
