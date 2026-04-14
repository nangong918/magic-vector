// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'oss_user_bucket_list_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

OssUserBucketListResponse _$OssUserBucketListResponseFromJson(
  Map<String, dynamic> json,
) => OssUserBucketListResponse(
  userId: json['userId'] as String?,
  bucketNameList: (json['bucketNameList'] as List<dynamic>?)
      ?.map((e) => e as String)
      .toList(),
);

Map<String, dynamic> _$OssUserBucketListResponseToJson(
  OssUserBucketListResponse instance,
) => <String, dynamic>{
  'userId': instance.userId,
  'bucketNameList': instance.bucketNameList,
};
