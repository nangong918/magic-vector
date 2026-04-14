// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'oss_file_content_update_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

OssFileContentUpdateResponse _$OssFileContentUpdateResponseFromJson(
  Map<String, dynamic> json,
) => OssFileContentUpdateResponse(
  fileId: userIdFromWireJson(json['fileId']),
  originFileName: json['originFileName'] as String?,
  url: json['url'] as String?,
  updated: json['updated'] as bool?,
  message: json['message'] as String?,
);

Map<String, dynamic> _$OssFileContentUpdateResponseToJson(
  OssFileContentUpdateResponse instance,
) => <String, dynamic>{
  'fileId': instance.fileId,
  'originFileName': instance.originFileName,
  'url': instance.url,
  'updated': instance.updated,
  'message': instance.message,
};
