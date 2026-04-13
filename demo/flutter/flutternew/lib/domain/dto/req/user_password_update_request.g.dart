// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'user_password_update_request.dart';

UserPasswordUpdateRequest _$UserPasswordUpdateRequestFromJson(
  Map<String, dynamic> json,
) =>
    UserPasswordUpdateRequest(
      userId: json['userId'] as String,
      oldPassword: json['oldPassword'] as String,
      newPassword: json['newPassword'] as String,
    );

Map<String, dynamic> _$UserPasswordUpdateRequestToJson(
  UserPasswordUpdateRequest instance,
) =>
    <String, dynamic>{
      'userId': instance.userId,
      'oldPassword': instance.oldPassword,
      'newPassword': instance.newPassword,
    };
