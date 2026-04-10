import 'package:json_annotation/json_annotation.dart';

// flutter pub run build_runner build
// 和原文件同目录生成 user_login_request.g.dart
part 'user_login_request.g.dart';

@JsonSerializable()
class UserLoginRequest {
  final String account;
  final String password;

  const UserLoginRequest({
    required this.account,
    required this.password,
  });

  // 由 .g.dart 文件实现的 fromJson 工厂方法
  factory UserLoginRequest.fromJson(Map<String, dynamic> json) =>
      _$UserLoginRequestFromJson(json);

  // 可选：如果需要转JSON，添加 toJson 方法（接口返回的响应一般不需要，请求类需要）
  Map<String, dynamic> toJson() => _$UserLoginRequestToJson(this);
}
