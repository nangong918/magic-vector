import 'package:json_annotation/json_annotation.dart';

part 'oss_user_bucket_file_ids_response.g.dart';

@JsonSerializable()
class OssUserBucketFileIdsResponse {
  final String? userId;
  final String? bucketName;
  final List<String>? fileIdList;

  const OssUserBucketFileIdsResponse({
    this.userId,
    this.bucketName,
    this.fileIdList,
  });

  factory OssUserBucketFileIdsResponse.fromJson(Map<String, dynamic> json) =>
      _$OssUserBucketFileIdsResponseFromJson(json);

  Map<String, dynamic> toJson() => _$OssUserBucketFileIdsResponseToJson(this);
}
