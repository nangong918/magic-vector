import 'package:json_annotation/json_annotation.dart';

import '../user_id_wire_json.dart';

part 'oss_user_delete_all_response.g.dart';

@JsonSerializable()
class OssUserDeleteAllResponse {
  @JsonKey(fromJson: userIdFromWireJson)
  final int? userId;
  final int? totalCount;
  final int? successCount;
  final int? failCount;
  final String? message;

  const OssUserDeleteAllResponse({
    this.userId,
    this.totalCount,
    this.successCount,
    this.failCount,
    this.message,
  });

  factory OssUserDeleteAllResponse.fromJson(Map<String, dynamic> json) =>
      _$OssUserDeleteAllResponseFromJson(json);

  Map<String, dynamic> toJson() => _$OssUserDeleteAllResponseToJson(this);
}
