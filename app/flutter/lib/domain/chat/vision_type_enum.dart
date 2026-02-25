

enum VisionTypeEnum {
  unknown("null"),
  image("image"),
  imageList("image_list"),
  video("video");

  final String code;
  const VisionTypeEnum(this.code);

  static VisionTypeEnum getVisionType(String code) {
    return values.firstWhere(
          (type) => type.code == code,
      orElse: () => VisionTypeEnum.unknown,
    );
  }

  // 为了方便使用，添加一个静态的默认值
  static const VisionTypeEnum defaultValue = VisionTypeEnum.image;

  // 扩展方法：判断是否为有效类型
  bool get isValid => this != VisionTypeEnum.unknown;

  // 转为字符串（用于序列化）
  @override
  String toString() => code;
}