// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'user_password_update_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

UserPasswordUpdateResponse _$UserPasswordUpdateResponseFromJson(
  Map<String, dynamic> json,
) => UserPasswordUpdateResponse(
  userId: userIdFromWireJson(json['userId']),
  updated: json['updated'] as bool?,
  message: json['message'] as String?,
);

Map<String, dynamic> _$UserPasswordUpdateResponseToJson(
  UserPasswordUpdateResponse instance,
) => <String, dynamic>{
  'userId': instance.userId,
  'updated': instance.updated,
  'message': instance.message,
};
