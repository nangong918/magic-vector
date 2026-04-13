// JSON wire uses decimal string for snowflake ids; Dart fields stay int.

int? userIdFromWireJson(dynamic value) {
  if (value == null) return null;
  if (value is int) return value;
  if (value is String) return int.tryParse(value);
  if (value is num) return value.toInt();
  return null;
}

int userIdFromWireJsonRequired(dynamic value) => userIdFromWireJson(value) ?? 0;

String userIdToWireJson(int value) => value.toString();

Object? nullableUserIdToWireJson(int? value) =>
    value == null ? null : userIdToWireJson(value);

List<int> wireIntListFromJson(dynamic json) {
  if (json == null) return const <int>[];
  if (json is! List) return const <int>[];
  final out = <int>[];
  for (final e in json) {
    final v = userIdFromWireJson(e);
    if (v != null) out.add(v);
  }
  return out;
}

List<String> wireIntListToJson(List<int> ids) =>
    ids.map(userIdToWireJson).toList();
