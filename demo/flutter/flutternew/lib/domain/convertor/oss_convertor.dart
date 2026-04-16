import '../dto/resp/oss_user_bucket_file_item_list_response.dart';
import '../dto/resp/oss_user_bucket_file_item_row.dart';
import '../dto/resp/oss_user_bucket_list_response.dart';
import '../model/oss/oss_bucket_file_item_model.dart';
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
}
