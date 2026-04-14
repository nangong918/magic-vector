// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'oss_user_bucket_file_item_list_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

OssUserBucketFileItemListResponse _$OssUserBucketFileItemListResponseFromJson(
  Map<String, dynamic> json,
) => OssUserBucketFileItemListResponse(
  userId: json['userId'] as String?,
  bucketName: json['bucketName'] as String?,
  items: (json['items'] as List<dynamic>?)
      ?.map((e) => OssUserBucketFileItemRow.fromJson(e as Map<String, dynamic>))
      .toList(),
);

Map<String, dynamic> _$OssUserBucketFileItemListResponseToJson(
  OssUserBucketFileItemListResponse instance,
) => <String, dynamic>{
  'userId': instance.userId,
  'bucketName': instance.bucketName,
  'items': instance.items,
};
