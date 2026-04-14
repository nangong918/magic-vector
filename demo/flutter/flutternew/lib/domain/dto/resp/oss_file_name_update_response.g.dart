// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'oss_file_name_update_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

OssFileNameUpdateResponse _$OssFileNameUpdateResponseFromJson(
  Map<String, dynamic> json,
) => OssFileNameUpdateResponse(
  fileId: userIdFromWireJson(json['fileId']),
  newFileName: json['newFileName'] as String?,
  updated: json['updated'] as bool?,
  message: json['message'] as String?,
);

Map<String, dynamic> _$OssFileNameUpdateResponseToJson(
  OssFileNameUpdateResponse instance,
) => <String, dynamic>{
  'fileId': instance.fileId,
  'newFileName': instance.newFileName,
  'updated': instance.updated,
  'message': instance.message,
};
