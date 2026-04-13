import 'package:json_annotation/json_annotation.dart';

import '../user_id_wire_json.dart';

part 'oss_file_content_update_response.g.dart';

@JsonSerializable()
class OssFileContentUpdateResponse {
  @JsonKey(fromJson: userIdFromWireJson)
  final int? fileId;
  final String? originFileName;
  final String? url;
  final bool? updated;
  final String? message;

  const OssFileContentUpdateResponse({
    this.fileId,
    this.originFileName,
    this.url,
    this.updated,
    this.message,
  });

  factory OssFileContentUpdateResponse.fromJson(Map<String, dynamic> json) =>
      _$OssFileContentUpdateResponseFromJson(json);

  Map<String, dynamic> toJson() => _$OssFileContentUpdateResponseToJson(this);
}
