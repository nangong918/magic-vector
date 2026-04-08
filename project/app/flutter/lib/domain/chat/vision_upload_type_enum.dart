


enum VisionUploadTypeEnum {
  unknown("unknown"),
  http("http"),
  wsFragment("ws-fragment"),
  rtmp("rtmp");

  final String code;
  const VisionUploadTypeEnum(this.code);

  static VisionUploadTypeEnum getVisionUploadType(String code) {
    return values.firstWhere(
          (type) => type.code == code,
      orElse: () => VisionUploadTypeEnum.unknown,
    );
  }

  // 为了方便使用，添加一个静态的默认值
  static const VisionUploadTypeEnum defaultValue = VisionUploadTypeEnum.http;

  // 扩展方法：判断是否为有效类型
  bool get isValid => this != VisionUploadTypeEnum.unknown;

  // 转为字符串（用于序列化）
  @override
  String toString() => code;
}