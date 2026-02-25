import 'package:json_annotation/json_annotation.dart';

// flutter pub run build_runner build
// 核心：添加 part 声明，指定生成的 .g.dart 文件路径（和当前文件同名）
part 'UserTestReq.g.dart';

// 添加序列化注解，告诉工具需要生成代码
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

  // 核心：声明工厂方法，由生成的 .g.dart 文件实现
  factory UserTestReq.fromJson(Map<String, dynamic> json) => _$UserTestReqFromJson(json);

  // 核心：声明 toJson 方法，由生成的 .g.dart 文件实现（替换你手动写的版本）
  Map<String, dynamic> toJson() => _$UserTestReqToJson(this);
}