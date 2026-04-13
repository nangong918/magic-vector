// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'oss_user_delete_all_response.dart';

OssUserDeleteAllResponse _$OssUserDeleteAllResponseFromJson(
  Map<String, dynamic> json,
) =>
    OssUserDeleteAllResponse(
      userId: userIdFromWireJson(json['userId']),
      totalCount: (json['totalCount'] as num?)?.toInt(),
      successCount: (json['successCount'] as num?)?.toInt(),
      failCount: (json['failCount'] as num?)?.toInt(),
      message: json['message'] as String?,
    );

Map<String, dynamic> _$OssUserDeleteAllResponseToJson(
  OssUserDeleteAllResponse instance,
) =>
    <String, dynamic>{
      'userId': nullableUserIdToWireJson(instance.userId),
      'totalCount': instance.totalCount,
      'successCount': instance.successCount,
      'failCount': instance.failCount,
      'message': instance.message,
    };
