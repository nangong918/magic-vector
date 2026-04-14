import 'package:json_annotation/json_annotation.dart';

part 'oss_user_bucket_file_item_row.g.dart';

@JsonSerializable()
class OssUserBucketFileItemRow {
  final String? fileId;
  final String? originFileName;
  final String? url;

  const OssUserBucketFileItemRow({
    this.fileId,
    this.originFileName,
    this.url,
  });

  factory OssUserBucketFileItemRow.fromJson(Map<String, dynamic> json) =>
      _$OssUserBucketFileItemRowFromJson(json);

  Map<String, dynamic> toJson() => _$OssUserBucketFileItemRowToJson(this);
}
