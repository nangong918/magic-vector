import 'package:json_annotation/json_annotation.dart';

import '../user_id_wire_json.dart';

// flutter pub run build_runner build
// 和原文件同目录生成 user_token_verify_response.g.dart
part 'user_token_verify_response.g.dart';

@JsonSerializable()
class UserTokenVerifyResponse {
  @JsonKey(fromJson: userIdFromWireJson)
  final int? userId;
  final bool? valid;
  final String? message;

  const UserTokenVerifyResponse({
    required this.userId,
    required this.valid,
    required this.message,
  });

  // 由 .g.dart 文件实现的 fromJson 工厂方法
  factory UserTokenVerifyResponse.fromJson(Map<String, dynamic> json) =>
      _$UserTokenVerifyResponseFromJson(json);

  // 如果需要缓存/调试输出，保留 toJson 方法
  Map<String, dynamic> toJson() => _$UserTokenVerifyResponseToJson(this);
}
