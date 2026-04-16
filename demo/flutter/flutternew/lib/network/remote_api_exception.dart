/// 对齐 Android [NetworkBusinessException]：业务码非成功或 data 为空。
class RemoteApiException implements Exception {
  RemoteApiException(this.code, this.message);

  final String code;
  final String message;

  @override
  String toString() => 'RemoteApiException($code): $message';
}
