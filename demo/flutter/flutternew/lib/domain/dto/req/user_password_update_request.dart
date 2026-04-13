import 'package:json_annotation/json_annotation.dart';

part 'user_password_update_request.g.dart';

/// Wire: [userId] is decimal string (matches Spring [UserPasswordUpdateRequest]).
@JsonSerializable()
class UserPasswordUpdateRequest {
  final String userId;
  final String oldPassword;
  final String newPassword;

  const UserPasswordUpdateRequest({
    required this.userId,
    required this.oldPassword,
    required this.newPassword,
  });

  factory UserPasswordUpdateRequest.fromJson(Map<String, dynamic> json) =>
      _$UserPasswordUpdateRequestFromJson(json);

  Map<String, dynamic> toJson() => _$UserPasswordUpdateRequestToJson(this);
}
