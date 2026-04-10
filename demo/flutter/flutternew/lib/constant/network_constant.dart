class NetworkConstant {
  const NetworkConstant._();

  static const String dns = '192.168.1.2';
  static const int port = 48888;
  static const String protocol = 'http';

  static const String baseUrl = '$protocol://$dns:$port';

  static const int connectTimeout = 10_000;
  static const int receiveTimeout = 10_000;
}
