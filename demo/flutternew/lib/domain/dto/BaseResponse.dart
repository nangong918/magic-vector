import 'package:json_annotation/json_annotation.dart';

// 生成 BaseResponse.g.dart（和原文件同目录）
part 'BaseResponse.g.dart';

/// 泛型响应基类的自动序列化配置
/// [T] 是响应数据类型（比如 UserTestResp）
@JsonSerializable(genericArgumentFactories: true) // 关键：开启泛型参数工厂支持
class BaseResponse<T> {
  final String code;
  final String? message;
  final T? data;

  const BaseResponse({
    required this.code,
    this.message,
    this.data,
  });

  // 保留你原有业务逻辑：判断是否请求成功
  bool get isSuccess => code == '200';

  /// 自动生成的 fromJson 工厂方法
  /// 注意：泛型类需要添加 genericArgumentFactories 参数，生成带类型工厂的方法
  factory BaseResponse.fromJson(
      Map<String, dynamic> json,
      T Function(Object? json) fromJsonT, // 泛型类型的解析工厂（比如 UserTestResp.fromJson）
      ) =>
      _$BaseResponseFromJson(json, fromJsonT);

  /// 自动生成的 toJson 方法
  /// 泛型类需要传入 toJsonT 处理 T 类型的序列化
  Map<String, dynamic> toJson(
      Object? Function(T value) toJsonT, // 泛型类型的序列化工厂（比如 UserTestResp.toJson）
      ) =>
      _$BaseResponseToJson(this, toJsonT);
}