import '../entity/user_entity.dart';
import '../model/user_session_model.dart';

/// 对齐 Android [UserConvertor]。
class UserConvertor {
  UserConvertor._();

  static UserEntity modelToEntity(UserSessionModel m) {
    return UserEntity(
      userId: m.userId,
      account: m.account,
      name: m.name,
      avatarUrl: m.avatarUrl,
      accessToken: m.accessToken,
      password: m.password,
      isCurrent: m.isCurrent,
      lastLoginAt: m.lastLoginAt,
    );
  }

  static UserSessionModel entityToModel(UserEntity e) {
    return UserSessionModel(
      userId: e.userId,
      account: e.account,
      name: e.name,
      avatarUrl: e.avatarUrl,
      accessToken: e.accessToken,
      password: e.password,
      isCurrent: e.isCurrent,
      lastLoginAt: e.lastLoginAt,
    );
  }

  static UserEntity fromRow(Map<String, Object?> row) => UserEntity.fromRow(row);

  static Map<String, Object?> entityToRow(UserEntity e) => e.toRow();
}
