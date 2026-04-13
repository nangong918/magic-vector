import 'package:json_annotation/json_annotation.dart';

import '../user_id_wire_json.dart';

part 'user_password_update_response.g.dart';

@JsonSerializable()
class UserPasswordUpdateResponse {
  @JsonKey(fromJson: userIdFromWireJson)
  final int? userId;
  final bool? updated;
  final String? message;

  const UserPasswordUpdateResponse({
    this.userId,
    this.updated,
    this.message,
  });

  factory UserPasswordUpdateResponse.fromJson(Map<String, dynamic> json) =>
      _$UserPasswordUpdateResponseFromJson(json);

  Map<String, dynamic> toJson() => _$UserPasswordUpdateResponseToJson(this);
}
