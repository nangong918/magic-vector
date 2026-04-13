import 'package:json_annotation/json_annotation.dart';

import '../user_id_wire_json.dart';

part 'oss_file_name_update_response.g.dart';

@JsonSerializable()
class OssFileNameUpdateResponse {
  @JsonKey(fromJson: userIdFromWireJson)
  final int? fileId;
  final String? newFileName;
  final bool? updated;
  final String? message;

  const OssFileNameUpdateResponse({
    this.fileId,
    this.newFileName,
    this.updated,
    this.message,
  });

  factory OssFileNameUpdateResponse.fromJson(Map<String, dynamic> json) =>
      _$OssFileNameUpdateResponseFromJson(json);

  Map<String, dynamic> toJson() => _$OssFileNameUpdateResponseToJson(this);
}
