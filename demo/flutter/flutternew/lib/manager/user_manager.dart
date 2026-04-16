import '../data_source/local/user_local_data_source.dart';
import '../data_source/remote/user_remote_api_source.dart';
import '../domain/model/user_session_model.dart';
import '../network/app_api.dart';

/// 对齐 Android [UserManager]：持有 Local / Remote，ViewModel 只依赖本类。
class UserManager {
  UserManager._({
    UserLocalDataSource? localDataSource,
    UserRemoteApiSource? remoteApiSource,
  })  : _local = localDataSource ?? UserLocalDataSource(),
        _remote = remoteApiSource ?? UserRemoteApiSource(AppApi.instance);

  static final UserManager instance = UserManager._();

  final UserLocalDataSource _local;
  final UserRemoteApiSource _remote;

  UserSessionModel? _cachedCurrent;

  /// 供 [AuthHeaderInterceptor] 同步读取；依赖 [getCurrentUser] / [saveCurrentUser] 先填充缓存。
  UserSessionModel? get currentSessionSync => _cachedCurrent;

  Future<UserSessionModel> loginRemote({
    required String account,
    required String password,
  }) =>
      _remote.login(account: account, password: password);

  Future<UserSessionModel> registerRemote({
    required String account,
    required String password,
    required String name,
  }) =>
      _remote.register(account: account, password: password, name: name);

  Future<bool> verifyAccessTokenRemote({
    required int userId,
    required String accessToken,
  }) =>
      _remote.verifyAccessToken(userId: userId, accessToken: accessToken);

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
    await _local.saveCurrentUser(current);
    _cachedCurrent = current;
  }

  Future<UserSessionModel?> getCurrentUser() async {
    final cached = _cachedCurrent;
    if (cached != null && cached.accessToken.isNotEmpty) return cached;
    final model = await _local.getCurrentUser();
    if (model == null) return null;
    _cachedCurrent = model;
    return model;
  }

  Future<List<UserSessionModel>> getAllUsers() async {
    final list = await _local.getAllUsers();
    return list
        .where(
          (e) => !(e.userId == 1 ||
              e.account == 'tourist' ||
              e.accessToken == 'tourist'),
        )
        .toList();
  }

  Future<void> clearCurrentUser() async {
    await _local.clearCurrentUser();
    _cachedCurrent = null;
  }
}
