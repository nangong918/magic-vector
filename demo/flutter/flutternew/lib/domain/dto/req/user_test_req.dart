import 'package:json_annotation/json_annotation.dart';

// flutter pub run build_runner build
// 和原文件同目录生成 user_test_req.g.dart
part 'user_test_req.g.dart';

@JsonSerializable()
class UserTestReq {
  final String account;
  final String password;
  final String name;

  const UserTestReq({
    required this.account,
    required this.password,
    required this.name,
  });

  // 由 .g.dart 文件实现的 fromJson 工厂方法
  factory UserTestReq.fromJson(Map<String, dynamic> json) =>
      _$UserTestReqFromJson(json);

  // 可选：如果需要转JSON，添加 toJson 方法（接口返回的响应一般不需要，请求类需要）
  Map<String, dynamic> toJson() => _$UserTestReqToJson(this);
}
