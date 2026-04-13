import 'package:json_annotation/json_annotation.dart';

import '../user_id_wire_json.dart';
import 'oss_upload_item_result.dart';

part 'oss_batch_upload_response.g.dart';

@JsonSerializable()
class OssBatchUploadResponse {
  @JsonKey(fromJson: userIdFromWireJson)
  final int? userId;
  final String? bucketName;
  final int? successCount;
  final int? failCount;
  final List<OssUploadItemResult>? items;

  const OssBatchUploadResponse({
    this.userId,
    this.bucketName,
    this.successCount,
    this.failCount,
    this.items,
  });

  factory OssBatchUploadResponse.fromJson(Map<String, dynamic> json) =>
      _$OssBatchUploadResponseFromJson(json);

  Map<String, dynamic> toJson() => _$OssBatchUploadResponseToJson(this);
}
