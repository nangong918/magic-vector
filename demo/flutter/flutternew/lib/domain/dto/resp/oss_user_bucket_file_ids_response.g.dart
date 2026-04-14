// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'oss_user_bucket_file_ids_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

OssUserBucketFileIdsResponse _$OssUserBucketFileIdsResponseFromJson(
  Map<String, dynamic> json,
) => OssUserBucketFileIdsResponse(
  userId: json['userId'] as String?,
  bucketName: json['bucketName'] as String?,
  fileIdList: (json['fileIdList'] as List<dynamic>?)
      ?.map((e) => e as String)
      .toList(),
);

Map<String, dynamic> _$OssUserBucketFileIdsResponseToJson(
  OssUserBucketFileIdsResponse instance,
) => <String, dynamic>{
  'userId': instance.userId,
  'bucketName': instance.bucketName,
  'fileIdList': instance.fileIdList,
};
