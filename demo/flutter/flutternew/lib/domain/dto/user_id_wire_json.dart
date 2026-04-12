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
