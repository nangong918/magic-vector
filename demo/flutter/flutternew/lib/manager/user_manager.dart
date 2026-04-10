import '../data/local/user_session_db.dart';
import '../domain/model/user_session_model.dart';

class UserManager {
  UserManager._();
  static final UserManager instance = UserManager._();

  UserSessionModel? _cachedCurrent;

  Future<void> saveCurrentUser(UserSessionModel session) async {
    final isTourist =
        session.userId == 1 ||
        session.account == 'tourist' ||
        session.accessToken == 'tourist';
    if (isTourist) {
      _cachedCurrent = session.copyWith(
        isCurrent: true,
        lastLoginAt: DateTime.now().millisecondsSinceEpoch,
      );
      return;
    }
    final loginAt = DateTime.now().millisecondsSinceEpoch;
    final current = session.copyWith(isCurrent: true, lastLoginAt: loginAt);
    final db = UserSessionDb.instance;
    await db.clearCurrentFlag();
    await db.upsert(_toRow(current));
    _cachedCurrent = current;
  }

  Future<UserSessionModel?> getCurrentUser() async {
    final cached = _cachedCurrent;
    if (cached != null && cached.accessToken.isNotEmpty) return cached;
    final row = await UserSessionDb.instance.getCurrent();
    if (row == null) return null;
    final model = _fromRow(row);
    _cachedCurrent = model;
    return model;
  }

  Future<List<UserSessionModel>> getAllUsers() async {
    final rows = await UserSessionDb.instance.getAll();
    return rows
        .map(_fromRow)
        .where((e) => !(e.userId == 1 || e.account == 'tourist' || e.accessToken == 'tourist'))
        .toList();
  }

  Future<void> clearCurrentUser() async {
    final db = UserSessionDb.instance;
    final current = await getCurrentUser();
    if (current == null) return;
    await db.clearCurrentFlag();
    await db.upsert(_toRow(current.copyWith(accessToken: '', isCurrent: false)));
    _cachedCurrent = null;
  }

  Map<String, Object?> _toRow(UserSessionModel m) {
    return {
      'user_id': m.userId,
      'account': m.account,
      'name': m.name,
      'avatar_url': m.avatarUrl,
      'access_token': m.accessToken,
      'password': m.password,
      'is_current': m.isCurrent ? 1 : 0,
      'last_login_at': m.lastLoginAt,
    };
  }

  UserSessionModel _fromRow(Map<String, Object?> row) {
    return UserSessionModel(
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
}
