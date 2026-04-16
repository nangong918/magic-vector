import '../../domain/convertor/user_convertor.dart';
import '../../domain/model/user_session_model.dart';
import 'user_session_db.dart';

/// 对齐 Android [UserLocalSource]：对外仅 [UserSessionModel]，内部用 Entity + [UserConvertor]。
class UserLocalDataSource {
  UserLocalDataSource({UserSessionDb? db}) : _db = db ?? UserSessionDb.instance;

  final UserSessionDb _db;

  Future<void> saveCurrentUser(UserSessionModel session) async {
    await _db.clearCurrentFlag();
    await _db.upsert(UserConvertor.modelToRow(session));
  }

  Future<UserSessionModel?> getCurrentUser() async {
    final row = await _db.getCurrent();
    if (row == null) return null;
    return UserConvertor.fromRowToModel(row);
  }

  Future<List<UserSessionModel>> getAllUsers() async {
    final rows = await _db.getAll();
    return rows.map(UserConvertor.fromRowToModel).toList();
  }

  Future<void> clearCurrentUser() async {
    final current = await getCurrentUser();
    if (current == null) return;
    await _db.clearCurrentFlag();
    await _db.upsert(
      UserConvertor.modelToRow(
        current.copyWith(accessToken: '', isCurrent: false),
      ),
    );
  }
}
