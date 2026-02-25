// 导入枚举
import '../chat/vision_upload_type_enum.dart';
import '../chat/vision_type_enum.dart';

// 常量 - 使用顶层类
class BaseConstant {
  // 私有构造函数，防止实例化
  BaseConstant._();
  // 静态成员，实现树状访问
  static const Constant constant = Constant._();
  static const HttpConstant http = HttpConstant._();
  static const ConstantUrl url = ConstantUrl._();
  static const WSConstantUrl wsUrl = WSConstantUrl._();
  static const NetworkCode networkCode = NetworkCode._();
  static const AudioConstant audio = AudioConstant._();
  static const YoloConstant yolo = YoloConstant._();
  static const VisionConstant vision = VisionConstant._();
  static const UdpConstant udp = UdpConstant._();
}

// 普通常量
class Constant {
  const Constant._(); // 注意这里是 const 构造函数

  static const Constant _instance = Constant._(); // 单例

  static Constant get instance => _instance;

  // 普通常量成员（注意：这些不是 static 的）
  final int startDelayTime = 1_200;
  // 头像最大大小 200 * 200 = 160 KB
  final int bitmapMaxSizeAvatar = 200;
  final int chatHistoryLimitCount = 20;
  final int maxAgentNameLength = 20;
}

// HTTP常量
class HttpConstant {
  const HttpConstant._();

  // 连接超时：2200ms
  final int connectTimeout = 2_200;
  // 读取超时：10s
  final int readTimeout = 10_000;
  // 写入超时：10s
  final int writeTimeout = 10_000;
  // 响应处理超时时间：30s
  final int callTimeout = 30_000;
}

// URL常量
class ConstantUrl {
  const ConstantUrl._();

  final String localHost = "192.168.1.2";
  final String testHost = "192.168.1.2";

  String get localAddress => "$localHost:48888";
  String get testAddress => "$testHost:48888";

  String get localUrl => "http://$localAddress";
  String get testUrl => "http://$testAddress";
  final String prodUrl = "https://api.vector.com";

  String get localWsUrl => "ws://$localAddress";
  String get testWsUrl => "ws://$testAddress";
  final String prodWsUrl = "wss://api.vector.com";
}

// WebSocket URL常量
class WSConstantUrl {
  const WSConstantUrl._();

  final String agentRealtimeChatUrl = "/agent/realtime/chat";
}

// 网络状态码
class NetworkCode {
  const NetworkCode._();

  final int success = 200;
  final String successCode = "200";
}

// 音频常量
class AudioConstant {
  const AudioConstant._();

  final int realtimeChatSampleRate = 24_000;
}

// YOLO常量
class YoloConstant {
  const YoloConstant._();

  final double filterSize = 0.1 * 0.1;
  final int personCls = 0;

  final double objectSdiffW = 0.1;
  final double personCountW = 0.1;
  final double objectCountW = 0.05;

  final double personThresholdValue = 0.10;
  final double objectThresholdValue = 0.40;
}

// 视觉常量
class VisionConstant {
  const VisionConstant._();

  // 这些是变量，可以修改
  final VisionUploadTypeEnum uploadMethod = VisionUploadTypeEnum.http;
  final VisionTypeEnum visionType = VisionTypeEnum.image;
  // 分片大小: 兼容ws和mqtt的最大限制
  final int fragmentSize = 16 * 1024; // 16kB大小
  final int wsShardUploadDelay = 20; // 分片上传延迟20ms，避免网络拥塞
}

// UDP常量
class UdpConstant {
  const UdpConstant._();

  final int port = 45000;
  final int maxPacketSize = 1_450;
  final int minFrameInterval = 100;
  final int bitmapQuality = 70;
  final int udpTimeout = 5_000;
}