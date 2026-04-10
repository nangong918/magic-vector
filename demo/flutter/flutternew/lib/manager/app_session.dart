class AppSession {
  AppSession._();
  static final AppSession instance = AppSession._();

  String _userId = '';

  String get userId => _userId;

  void updateUserId(int userId) {
    _userId = userId > 0 ? userId.toString() : '';
  }

  void clearUserId() {
    _userId = '';
  }
}
