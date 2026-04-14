import 'package:json_annotation/json_annotation.dart';

import 'oss_user_bucket_file_item_row.dart';

part 'oss_user_bucket_file_item_list_response.g.dart';

@JsonSerializable()
class OssUserBucketFileItemListResponse {
  final String? userId;
  final String? bucketName;
  final List<OssUserBucketFileItemRow>? items;

  const OssUserBucketFileItemListResponse({
    this.userId,
    this.bucketName,
    this.items,
  });

  factory OssUserBucketFileItemListResponse.fromJson(Map<String, dynamic> json) =>
      _$OssUserBucketFileItemListResponseFromJson(json);

  Map<String, dynamic> toJson() => _$OssUserBucketFileItemListResponseToJson(this);
}
