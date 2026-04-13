import 'package:json_annotation/json_annotation.dart';

import '../user_id_wire_json.dart';

part 'oss_upload_item_result.g.dart';

@JsonSerializable()
class OssUploadItemResult {
  final String? originFileName;
  final bool success;
  final bool duplicated;
  @JsonKey(fromJson: userIdFromWireJson)
  final int? fileId;
  final String? url;
  final String? message;

  const OssUploadItemResult({
    this.originFileName,
    required this.success,
    required this.duplicated,
    this.fileId,
    this.url,
    this.message,
  });

  factory OssUploadItemResult.fromJson(Map<String, dynamic> json) =>
      _$OssUploadItemResultFromJson(json);

  Map<String, dynamic> toJson() => _$OssUploadItemResultToJson(this);
}
