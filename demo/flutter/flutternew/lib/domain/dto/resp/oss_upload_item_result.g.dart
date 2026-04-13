// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'oss_upload_item_result.dart';

OssUploadItemResult _$OssUploadItemResultFromJson(Map<String, dynamic> json) =>
    OssUploadItemResult(
      originFileName: json['originFileName'] as String?,
      success: json['success'] as bool? ?? false,
      duplicated: json['duplicated'] as bool? ?? false,
      fileId: userIdFromWireJson(json['fileId']),
      url: json['url'] as String?,
      message: json['message'] as String?,
    );

Map<String, dynamic> _$OssUploadItemResultToJson(OssUploadItemResult instance) =>
    <String, dynamic>{
      'originFileName': instance.originFileName,
      'success': instance.success,
      'duplicated': instance.duplicated,
      'fileId': nullableUserIdToWireJson(instance.fileId),
      'url': instance.url,
      'message': instance.message,
    };
