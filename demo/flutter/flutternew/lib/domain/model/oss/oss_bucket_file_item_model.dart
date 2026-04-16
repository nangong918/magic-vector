class OssBucketFileItemModel {
  final int fileId;
  final String originFileName;
  final String url;

  const OssBucketFileItemModel({
    required this.fileId,
    required this.originFileName,
    required this.url,
  });

  String get fileIdWire => fileId.toString();
}
