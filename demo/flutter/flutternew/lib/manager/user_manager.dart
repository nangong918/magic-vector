import '../data/local/user_local_data_source.dart';
import '../data/remote/user_remote_data_source.dart';
import '../domain/convertor/user_convertor.dart';
import '../domain/dto/req/user_login_request.dart';
import '../domain/dto/req/user_token_verify_request.dart';
import '../domain/dto/resp/user_auth_response.dart';
import '../domain/dto/resp/user_token_verify_response.dart';
import '../domain/model/user_session_model.dart';
import '../network/app_api.dart';

/// 对齐 Android [UserManager]：持有 Local / Remote [DataSource]，ViewModel 只依赖本类。
class UserManager {
  UserManager._({
    UserLocalDataSource? localDataSource,
    UserRemoteDataSource? remoteDataSource,
  })  : _local = localDataSource ?? UserLocalDataSource(),
        _remote = remoteDataSource ?? UserRemoteDataSource(AppApi.instance);

  static final UserManager instance = UserManager._();

  final UserLocalDataSource _local;
  final UserRemoteDataSource _remote;

  UserSessionModel? _cachedCurrent;

  /// 供 [AuthHeaderInterceptor] 同步读取；依赖 [getCurrentUser] / [saveCurrentUser] 先填充缓存。
  UserSessionModel? get currentSessionSync => _cachedCurrent;

  Future<UserAuthResponse> loginRemote(UserLoginRequest request) =>
      _remote.login(request);

  Future<UserAuthResponse> registerRemote({
    required String account,
    required String password,
    required String name,
  }) =>
      _remote.register(account: account, password: password, name: name);

  Future<UserTokenVerifyResponse> verifyAccessTokenRemote(
    UserTokenVerifyRequest request,
  ) =>
      _remote.verifyAccessToken(request);

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
    await _local.saveCurrentUser(UserConvertor.modelToEntity(current));
    _cachedCurrent = current;
  }

  Future<UserSessionModel?> getCurrentUser() async {
    final cached = _cachedCurrent;
    if (cached != null && cached.accessToken.isNotEmpty) return cached;
    final entity = await _local.getCurrentUser();
    if (entity == null) return null;
    final model = UserConvertor.entityToModel(entity);
    _cachedCurrent = model;
    return model;
  }

  Future<List<UserSessionModel>> getAllUsers() async {
    final entities = await _local.getAllUsers();
    return entities
        .map(UserConvertor.entityToModel)
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
