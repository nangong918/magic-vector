// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'oss_user_bucket_file_item_row.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

OssUserBucketFileItemRow _$OssUserBucketFileItemRowFromJson(
  Map<String, dynamic> json,
) => OssUserBucketFileItemRow(
  fileId: json['fileId'] as String?,
  originFileName: json['originFileName'] as String?,
  url: json['url'] as String?,
);

Map<String, dynamic> _$OssUserBucketFileItemRowToJson(
  OssUserBucketFileItemRow instance,
) => <String, dynamic>{
  'fileId': instance.fileId,
  'originFileName': instance.originFileName,
  'url': instance.url,
};
