class UserLoginRequest {
  final String account;
  final String password;

  UserLoginRequest({
    required this.account,
    required this.password,
  });

  Map<String, dynamic> toJson() {
    return {
      'account': account,
      'password': password,
    };
  }
}
