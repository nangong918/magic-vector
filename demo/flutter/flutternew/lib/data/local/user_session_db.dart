import 'package:path/path.dart' as path_util;
import 'package:sqflite/sqflite.dart';

class UserSessionDb {
  static final UserSessionDb instance = UserSessionDb._();
  UserSessionDb._();

  static const _table = 'user_session';
  Database? _db;

  Future<Database> get database async {
    if (_db != null) return _db!;
    final dbPath = await getDatabasesPath();
    _db = await openDatabase(
      path_util.join(dbPath, 'flutternew_auth.db'),
      version: 1,
      onCreate: (db, _) async {
        await db.execute('''
          CREATE TABLE $_table (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL,
            account TEXT NOT NULL UNIQUE,
            name TEXT NOT NULL,
            avatar_url TEXT NOT NULL,
            access_token TEXT NOT NULL,
            password TEXT NOT NULL,
            is_current INTEGER NOT NULL,
            last_login_at INTEGER NOT NULL
          )
        ''');
      },
    );
    return _db!;
  }

  Future<void> clearCurrentFlag() async {
    final db = await database;
    await db.update(_table, {'is_current': 0});
  }

  Future<void> upsert(Map<String, Object?> row) async {
    final db = await database;
    await db.insert(_table, row, conflictAlgorithm: ConflictAlgorithm.replace);
  }

  Future<Map<String, Object?>?> getCurrent() async {
    final db = await database;
    final rows = await db.query(
      _table,
      where: 'is_current = ?',
      whereArgs: [1],
      orderBy: 'last_login_at DESC',
      limit: 1,
    );
    if (rows.isEmpty) return null;
    return rows.first;
  }

  Future<List<Map<String, Object?>>> getAll() async {
    final db = await database;
    return db.query(_table, orderBy: 'is_current DESC, last_login_at DESC, id DESC');
  }
}
