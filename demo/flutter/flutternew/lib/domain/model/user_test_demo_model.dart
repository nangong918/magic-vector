/// Demo `/test/network/*` 业务模型；ViewModel 不直接依赖 [UserTestResp]。
class UserTestDemoModel {
  final String account;
  final String loginToken;

  const UserTestDemoModel({
    required this.account,
    required this.loginToken,
  });
}
