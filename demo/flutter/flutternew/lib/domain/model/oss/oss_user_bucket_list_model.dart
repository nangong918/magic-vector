class OssUserBucketListModel {
  final int userId;
  final List<String> bucketNames;

  const OssUserBucketListModel({
    required this.userId,
    required this.bucketNames,
  });
}
