import 'package:json_annotation/json_annotation.dart';

import '../user_id_wire_json.dart';

part 'oss_batch_delete_response.g.dart';

@JsonSerializable()
class OssBatchDeleteResponse {
  @JsonKey(fromJson: wireIntListFromJson)
  final List<int> fileIdList;
  final int? successCount;
  final int? failCount;
  final String? message;

  const OssBatchDeleteResponse({
    required this.fileIdList,
    this.successCount,
    this.failCount,
    this.message,
  });

  factory OssBatchDeleteResponse.fromJson(Map<String, dynamic> json) =>
      _$OssBatchDeleteResponseFromJson(json);

  Map<String, dynamic> toJson() => _$OssBatchDeleteResponseToJson(this);
}
