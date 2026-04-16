class OssFileContentUpdateModel {
  final String fileId;
  final String originFileName;
  final String url;
  final bool updated;
  final String message;

  const OssFileContentUpdateModel({
    required this.fileId,
    required this.originFileName,
    required this.url,
    required this.updated,
    required this.message,
  });
}
