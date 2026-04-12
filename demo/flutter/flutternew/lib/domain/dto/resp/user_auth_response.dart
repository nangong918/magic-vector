import 'package:json_annotation/json_annotation.dart';

import '../user_id_wire_json.dart';

// flutter pub run build_runner build
// 和原文件同目录生成 user_auth_response.g.dart
part 'user_auth_response.g.dart';

@JsonSerializable()
class UserAuthResponse {
  @JsonKey(fromJson: userIdFromWireJson)
  final int? userId;
  final String? account;
  final String? name;
  final String? avatarUrl;
  final String? accessToken;

  const UserAuthResponse({
    required this.userId,
    required this.account,
    required this.name,
    required this.avatarUrl,
    required this.accessToken,
  });

  // 由 .g.dart 文件实现的 fromJson 工厂方法
  factory UserAuthResponse.fromJson(Map<String, dynamic> json) =>
      _$UserAuthResponseFromJson(json);

  // 如果需要缓存/调试输出，保留 toJson 方法
  Map<String, dynamic> toJson() => _$UserAuthResponseToJson(this);
}
