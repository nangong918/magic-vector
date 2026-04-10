class UserSessionModel {
  final int userId;
  final String account;
  final String name;
  final String avatarUrl;
  final String accessToken;
  final String password;
  final bool isCurrent;
  final int lastLoginAt;

  const UserSessionModel({
    required this.userId,
    required this.account,
    required this.name,
    required this.avatarUrl,
    required this.accessToken,
    this.password = '',
    this.isCurrent = false,
    this.lastLoginAt = 0,
  });

  bool get isEmpty => userId <= 0 && account.isEmpty;

  UserSessionModel copyWith({
    int? userId,
    String? account,
    String? name,
    String? avatarUrl,
    String? accessToken,
    String? password,
    bool? isCurrent,
    int? lastLoginAt,
  }) {
    return UserSessionModel(
      userId: userId ?? this.userId,
      account: account ?? this.account,
      name: name ?? this.name,
      avatarUrl: avatarUrl ?? this.avatarUrl,
      accessToken: accessToken ?? this.accessToken,
      password: password ?? this.password,
      isCurrent: isCurrent ?? this.isCurrent,
      lastLoginAt: lastLoginAt ?? this.lastLoginAt,
    );
  }
}
