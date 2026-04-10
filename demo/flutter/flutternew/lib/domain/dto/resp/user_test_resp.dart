import 'package:json_annotation/json_annotation.dart';

part 'user_test_resp.g.dart';

// flutter pub run build_runner build
// 核心：添加 part 声明，指定生成的 .g.dart 文件路径（和当前文件同名）
@JsonSerializable()
class UserTestResp {
  final String? account;
  final String? loginToken;

  const UserTestResp({
    this.account,
    this.loginToken,
  });

  // 核心：声明工厂方法，由生成的 .g.dart 文件实现
  factory UserTestResp.fromJson(Map<String, dynamic> json) =>
      _$UserTestRespFromJson(json);

  // 核心：声明 toJson 方法，由生成的 .g.dart 文件实现（替换你手动写的版本）
  Map<String, dynamic> toJson() => _$UserTestRespToJson(this);
}
