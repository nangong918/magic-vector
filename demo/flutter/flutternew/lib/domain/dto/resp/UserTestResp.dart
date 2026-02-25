import 'package:json_annotation/json_annotation.dart';

// flutter pub run build_runner build
// 和原文件同目录生成 UserTestResp.g.dart
part 'UserTestResp.g.dart';

// 标记需要自动生成序列化代码
@JsonSerializable()
class UserTestResp {
  // 可空字段无需额外处理，json_serializable 会自动兼容
  final String? account;
  final String? loginToken;

  const UserTestResp({
    this.account,
    this.loginToken,
  });

  // 由 .g.dart 文件实现的 fromJson 工厂方法
  factory UserTestResp.fromJson(Map<String, dynamic> json) => _$UserTestRespFromJson(json);

  // 可选：如果需要转JSON，添加 toJson 方法（接口返回的响应一般不需要，请求类需要）
  Map<String, dynamic> toJson() => _$UserTestRespToJson(this);
}