class UserTokenVerifyRequest {
  final int userId;
  final String accessToken;

  UserTokenVerifyRequest({
    required this.userId,
    required this.accessToken,
  });

  Map<String, dynamic> toJson() {
    return {
      'userId': userId,
      'accessToken': accessToken,
    };
  }
}
