class UserAuthResponse {
  final int? userId;
  final String? account;
  final String? name;
  final String? avatarUrl;
  final String? accessToken;

  UserAuthResponse({
    required this.userId,
    required this.account,
    required this.name,
    required this.avatarUrl,
    required this.accessToken,
  });

  factory UserAuthResponse.fromJson(Map<String, dynamic> json) {
    return UserAuthResponse(
      userId: (json['userId'] as num?)?.toInt(),
      account: json['account'] as String?,
      name: json['name'] as String?,
      avatarUrl: json['avatarUrl'] as String?,
      accessToken: json['accessToken'] as String?,
    );
  }
}
