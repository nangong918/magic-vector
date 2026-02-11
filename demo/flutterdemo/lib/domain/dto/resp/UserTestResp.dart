class UserTestResp {
  final String? account;
  final String? loginToken;

  const UserTestResp({
    this.account,
    this.loginToken,
  });

  factory UserTestResp.fromJson(Object? json) {
    final map = json as Map<String, dynamic>;
    return UserTestResp(
      account: map['account']?.toString(),
      loginToken: map['loginToken']?.toString(),
    );
  }
}
