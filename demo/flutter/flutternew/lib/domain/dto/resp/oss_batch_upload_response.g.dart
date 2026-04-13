// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'oss_batch_upload_response.dart';

OssBatchUploadResponse _$OssBatchUploadResponseFromJson(
  Map<String, dynamic> json,
) =>
    OssBatchUploadResponse(
      userId: userIdFromWireJson(json['userId']),
      bucketName: json['bucketName'] as String?,
      successCount: (json['successCount'] as num?)?.toInt(),
      failCount: (json['failCount'] as num?)?.toInt(),
      items: (json['items'] as List<dynamic>?)
          ?.map((e) => OssUploadItemResult.fromJson(e as Map<String, dynamic>))
          .toList(),
    );

Map<String, dynamic> _$OssBatchUploadResponseToJson(
  OssBatchUploadResponse instance,
) =>
    <String, dynamic>{
      'userId': nullableUserIdToWireJson(instance.userId),
      'bucketName': instance.bucketName,
      'successCount': instance.successCount,
      'failCount': instance.failCount,
      'items': instance.items?.map((e) => e.toJson()).toList(),
    };
