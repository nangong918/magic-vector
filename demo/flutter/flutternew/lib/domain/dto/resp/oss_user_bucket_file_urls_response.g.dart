// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'oss_user_bucket_file_urls_response.dart';

OssUserBucketFileUrlsResponse _$OssUserBucketFileUrlsResponseFromJson(
        Map<String, dynamic> json) =>
    OssUserBucketFileUrlsResponse(
      userId: json['userId'] as String?,
      bucketName: json['bucketName'] as String?,
      urlList: (json['urlList'] as List<dynamic>?)
          ?.map((e) => e as String)
          .toList(),
    );

Map<String, dynamic> _$OssUserBucketFileUrlsResponseToJson(
        OssUserBucketFileUrlsResponse instance) =>
    <String, dynamic>{
      'userId': instance.userId,
      'bucketName': instance.bucketName,
      'urlList': instance.urlList,
    };
