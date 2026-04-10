class UserTokenVerifyResponse {
  final int? userId;
  final bool? valid;
  final String? message;

  UserTokenVerifyResponse({
    required this.userId,
    required this.valid,
    required this.message,
  });

  factory UserTokenVerifyResponse.fromJson(Map<String, dynamic> json) {
    return UserTokenVerifyResponse(
      userId: (json['userId'] as num?)?.toInt(),
      valid: json['valid'] as bool?,
      message: json['message'] as String?,
    );
  }
}
