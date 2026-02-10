class NetworkConstant {
  const NetworkConstant._();

  static const String dns = '192.168.3.140';
  static const int port = 58888;
  static const String protocol = 'http';

  static String get baseUrl => '$protocol://$dns:$port';
}
