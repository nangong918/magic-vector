class UserTestReq {
  final String account;
  final String password;
  final String name;

  const UserTestReq({
    required this.account,
    required this.password,
    required this.name,
  });

  Map<String, dynamic> toJson() {
    return {
      'account': account,
      'password': password,
      'name': name,
    };
  }
}
