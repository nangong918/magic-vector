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
  ) {
    return BaseResponse(
      code: json['code']?.toString() ?? '',
      message: json['message']?.toString(),
      data: json['data'] == null ? null : fromJsonT(json['data']),
    );
  }

  Map<String, dynamic> toJson(Object? Function(T value) toJsonT) {
    return {
      'code': code,
      'message': message,
      'data': data == null ? null : toJsonT(data as T),
    };
  }
}
