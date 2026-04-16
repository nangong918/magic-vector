/// 对齐 Android [UserEntity] / Room `user_session` 表字段。
class UserEntity {
  final int id;
  final int userId;
  final String account;
  final String name;
  final String avatarUrl;
  final String accessToken;
  final String password;
  final bool isCurrent;
  final int lastLoginAt;

  const UserEntity({
    this.id = 0,
    required this.userId,
    required this.account,
    required this.name,
    required this.avatarUrl,
    required this.accessToken,
    required this.password,
    required this.isCurrent,
    required this.lastLoginAt,
  });

  factory UserEntity.fromRow(Map<String, Object?> row) {
    return UserEntity(
      id: (row['id'] as num?)?.toInt() ?? 0,
      userId: (row['user_id'] as num?)?.toInt() ?? 0,
      account: row['account'] as String? ?? '',
      name: row['name'] as String? ?? '',
      avatarUrl: row['avatar_url'] as String? ?? '',
      accessToken: row['access_token'] as String? ?? '',
      password: row['password'] as String? ?? '',
      isCurrent: ((row['is_current'] as num?)?.toInt() ?? 0) == 1,
      lastLoginAt: (row['last_login_at'] as num?)?.toInt() ?? 0,
    );
  }

  Map<String, Object?> toRow() {
    return {
      'user_id': userId,
      'account': account,
      'name': name,
      'avatar_url': avatarUrl,
      'access_token': accessToken,
      'password': password,
      'is_current': isCurrent ? 1 : 0,
      'last_login_at': lastLoginAt,
    };
  }

  UserEntity copyWith({
    int? id,
    int? userId,
    String? account,
    String? name,
    String? avatarUrl,
    String? accessToken,
    String? password,
    bool? isCurrent,
    int? lastLoginAt,
  }) {
    return UserEntity(
      id: id ?? this.id,
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
