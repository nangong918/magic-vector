import '../dto/resp/user_auth_response.dart';
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

  static UserSessionModel fromRowToModel(Map<String, Object?> row) =>
      entityToModel(fromRow(row));

  static Map<String, Object?> modelToRow(UserSessionModel m) =>
      entityToRow(modelToEntity(m));

  static UserSessionModel authResponseToSessionModel(
    UserAuthResponse auth,
    String password,
  ) {
    final now = DateTime.now().millisecondsSinceEpoch;
    return UserSessionModel(
      userId: auth.userId ?? 0,
      account: auth.account ?? '',
      name: auth.name ?? '',
      avatarUrl: auth.avatarUrl ?? '',
      accessToken: auth.accessToken ?? '',
      password: password,
      isCurrent: true,
      lastLoginAt: now,
    );
  }
}
