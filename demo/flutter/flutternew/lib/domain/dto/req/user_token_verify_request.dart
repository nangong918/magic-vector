import 'package:json_annotation/json_annotation.dart';

import '../user_id_wire_json.dart';

// flutter pub run build_runner build
// 和原文件同目录生成 user_token_verify_request.g.dart
part 'user_token_verify_request.g.dart';

@JsonSerializable()
class UserTokenVerifyRequest {
  @JsonKey(fromJson: userIdFromWireJsonRequired, toJson: userIdToWireJson)
  final int userId;
  final String accessToken;

  const UserTokenVerifyRequest({
    required this.userId,
    required this.accessToken,
  });

  // 由 .g.dart 文件实现的 fromJson 工厂方法
  factory UserTokenVerifyRequest.fromJson(Map<String, dynamic> json) =>
      _$UserTokenVerifyRequestFromJson(json);

  // 可选：如果需要转JSON，添加 toJson 方法（接口返回的响应一般不需要，请求类需要）
  Map<String, dynamic> toJson() => _$UserTokenVerifyRequestToJson(this);
}
