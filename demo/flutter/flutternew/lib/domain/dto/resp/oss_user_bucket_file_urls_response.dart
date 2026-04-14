import 'package:json_annotation/json_annotation.dart';

part 'oss_user_bucket_file_urls_response.g.dart';

@JsonSerializable()
class OssUserBucketFileUrlsResponse {
  final String? userId;
  final String? bucketName;
  final List<String>? urlList;

  const OssUserBucketFileUrlsResponse({
    this.userId,
    this.bucketName,
    this.urlList,
  });

  factory OssUserBucketFileUrlsResponse.fromJson(Map<String, dynamic> json) =>
      _$OssUserBucketFileUrlsResponseFromJson(json);

  Map<String, dynamic> toJson() => _$OssUserBucketFileUrlsResponseToJson(this);
}
