import 'package:json_annotation/json_annotation.dart';

part 'base_response.g.dart';

/// 泛型响应基类的自动序列化配置
@JsonSerializable(genericArgumentFactories: true)
class BaseResponse<T> {
  final String code;
  final String? message;
  final T? data;

  const BaseResponse({
    required this.code,
    this.message,
    this.data,
  });

  bool get isSuccess => code == '200';

  factory BaseResponse.fromJson(
    Map<String, dynamic> json,
    T Function(Object? json) fromJsonT,
  ) =>
      _$BaseResponseFromJson(json, fromJsonT);

  Map<String, dynamic> toJson(
    Object? Function(T value) toJsonT,
  ) =>
      _$BaseResponseToJson(this, toJsonT);
}
