import '../../domain/convertor/user_convertor.dart';
import '../../domain/entity/user_entity.dart';
import 'user_session_db.dart';

/// 对齐 Android [UserLocalSource]：仅暴露 Entity，屏蔽表结构细节。
class UserLocalDataSource {
  UserLocalDataSource({UserSessionDb? db}) : _db = db ?? UserSessionDb.instance;

  final UserSessionDb _db;

  Future<void> saveCurrentUser(UserEntity entity) async {
    await _db.clearCurrentFlag();
    await _db.upsert(UserConvertor.entityToRow(entity));
  }

  Future<UserEntity?> getCurrentUser() async {
    final row = await _db.getCurrent();
    if (row == null) return null;
    return UserConvertor.fromRow(row);
  }

  Future<List<UserEntity>> getAllUsers() async {
    final rows = await _db.getAll();
    return rows.map(UserConvertor.fromRow).toList();
  }

  Future<void> clearCurrentUser() async {
    final current = await getCurrentUser();
    if (current == null) return;
    await _db.clearCurrentFlag();
    await _db.upsert(
      UserConvertor.entityToRow(
        current.copyWith(accessToken: '', isCurrent: false),
      ),
    );
  }
}
