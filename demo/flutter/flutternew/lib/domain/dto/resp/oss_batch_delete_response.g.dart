// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'oss_batch_delete_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

OssBatchDeleteResponse _$OssBatchDeleteResponseFromJson(
  Map<String, dynamic> json,
) => OssBatchDeleteResponse(
  fileIdList: wireIntListFromJson(json['fileIdList']),
  successCount: (json['successCount'] as num?)?.toInt(),
  failCount: (json['failCount'] as num?)?.toInt(),
  message: json['message'] as String?,
);

Map<String, dynamic> _$OssBatchDeleteResponseToJson(
  OssBatchDeleteResponse instance,
) => <String, dynamic>{
  'fileIdList': instance.fileIdList,
  'successCount': instance.successCount,
  'failCount': instance.failCount,
  'message': instance.message,
};
