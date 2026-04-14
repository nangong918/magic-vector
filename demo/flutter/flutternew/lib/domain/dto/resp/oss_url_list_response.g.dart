// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'oss_url_list_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

OssUrlListResponse _$OssUrlListResponseFromJson(Map<String, dynamic> json) =>
    OssUrlListResponse(
      fileIdList: wireIntListFromJson(json['fileIdList']),
      urlList: (json['urlList'] as List<dynamic>?)
          ?.map((e) => e as String)
          .toList(),
    );

Map<String, dynamic> _$OssUrlListResponseToJson(OssUrlListResponse instance) =>
    <String, dynamic>{
      'fileIdList': instance.fileIdList,
      'urlList': instance.urlList,
    };
