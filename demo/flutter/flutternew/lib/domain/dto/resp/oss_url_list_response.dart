import 'package:json_annotation/json_annotation.dart';

import '../user_id_wire_json.dart';

part 'oss_url_list_response.g.dart';

@JsonSerializable()
class OssUrlListResponse {
  @JsonKey(fromJson: wireIntListFromJson)
  final List<int> fileIdList;
  final List<String>? urlList;

  const OssUrlListResponse({
    required this.fileIdList,
    this.urlList,
  });

  factory OssUrlListResponse.fromJson(Map<String, dynamic> json) =>
      _$OssUrlListResponseFromJson(json);

  Map<String, dynamic> toJson() => _$OssUrlListResponseToJson(this);
}
