// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'user_auth_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

UserAuthResponse _$UserAuthResponseFromJson(Map<String, dynamic> json) =>
    UserAuthResponse(
      userId: userIdFromWireJson(json['userId']),
      account: json['account'] as String?,
      name: json['name'] as String?,
      avatarUrl: json['avatarUrl'] as String?,
      accessToken: json['accessToken'] as String?,
    );

Map<String, dynamic> _$UserAuthResponseToJson(UserAuthResponse instance) =>
    <String, dynamic>{
      'userId': instance.userId,
      'account': instance.account,
      'name': instance.name,
      'avatarUrl': instance.avatarUrl,
      'accessToken': instance.accessToken,
    };
