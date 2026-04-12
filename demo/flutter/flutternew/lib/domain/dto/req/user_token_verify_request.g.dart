// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'user_token_verify_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

UserTokenVerifyRequest _$UserTokenVerifyRequestFromJson(
  Map<String, dynamic> json,
) => UserTokenVerifyRequest(
  userId: userIdFromWireJsonRequired(json['userId']),
  accessToken: json['accessToken'] as String,
);

Map<String, dynamic> _$UserTokenVerifyRequestToJson(
  UserTokenVerifyRequest instance,
) => <String, dynamic>{
  'userId': userIdToWireJson(instance.userId),
  'accessToken': instance.accessToken,
};
