// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'user_token_verify_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

UserTokenVerifyResponse _$UserTokenVerifyResponseFromJson(
  Map<String, dynamic> json,
) => UserTokenVerifyResponse(
  userId: userIdFromWireJson(json['userId']),
  valid: json['valid'] as bool?,
  message: json['message'] as String?,
);

Map<String, dynamic> _$UserTokenVerifyResponseToJson(
  UserTokenVerifyResponse instance,
) => <String, dynamic>{
  'userId': instance.userId,
  'valid': instance.valid,
  'message': instance.message,
};
