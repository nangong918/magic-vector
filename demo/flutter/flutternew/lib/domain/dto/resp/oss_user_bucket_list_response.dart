import 'package:json_annotation/json_annotation.dart';

part 'oss_user_bucket_list_response.g.dart';

@JsonSerializable()
class OssUserBucketListResponse {
  final String? userId;
  final List<String>? bucketNameList;

  const OssUserBucketListResponse({
    this.userId,
    this.bucketNameList,
  });

  factory OssUserBucketListResponse.fromJson(Map<String, dynamic> json) =>
      _$OssUserBucketListResponseFromJson(json);

  Map<String, dynamic> toJson() => _$OssUserBucketListResponseToJson(this);
}
