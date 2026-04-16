class OssUploadItemModel {
  final String originFileName;
  final bool success;
  final bool duplicated;
  final String fileId;
  final String url;
  final String message;

  const OssUploadItemModel({
    required this.originFileName,
    required this.success,
    required this.duplicated,
    required this.fileId,
    required this.url,
    required this.message,
  });
}
