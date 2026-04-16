import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

/// 对齐 Android：仅当请求/响应为 JSON 时打印 body；multipart / 非 JSON 不打印正文。
class SafeDioLogInterceptor extends Interceptor {
  static bool _isJsonContentType(String? ct) {
    if (ct == null || ct.isEmpty) return false;
    final lower = ct.toLowerCase();
    return lower.contains('application/json') ||
        lower.contains('application/problem+json') ||
        lower.contains('+json');
  }

  static bool _multipartWithFiles(FormData fd) => fd.files.isNotEmpty;

  static String? _tryJsonEncode(dynamic data) {
    if (data == null) return null;
    if (data is String) {
      final t = data.trimLeft();
      if (t.startsWith('{') || t.startsWith('[')) {
        try {
          jsonDecode(data);
          return data;
        } catch (_) {
          return null;
        }
      }
      return null;
    }
    if (data is Map || data is List) {
      try {
        return jsonEncode(data);
      } catch (_) {
        return null;
      }
    }
    return null;
  }

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    final ct = options.headers['content-type']?.toString() ??
        options.contentType?.toString();
    debugPrint('[Dio] --> ${options.method} ${options.uri}');
    if (options.headers.isNotEmpty) {
      debugPrint('[Dio] req headers: ${options.headers}');
    }
    final data = options.data;
    if (data is FormData) {
      final fd = data;
      if (_multipartWithFiles(fd)) {
        final keys = fd.files.map((e) => e.key).join(', ');
        debugPrint(
          '[Dio] req body: multipart/form-data (${fd.fields.length} fields, '
          'files: [$keys]; binary omitted)',
        );
      } else {
        final fieldMap = Map<String, dynamic>.fromEntries(fd.fields);
        final encoded = _tryJsonEncode(fieldMap);
        if (encoded != null && _isJsonContentType(ct)) {
          debugPrint('[Dio] req body (json): $encoded');
        } else {
          debugPrint('[Dio] req body: form fields $fieldMap (non-json, omitted detail)');
        }
      }
    } else if (_isJsonContentType(ct)) {
      final s = _tryJsonEncode(data);
      if (s != null) {
        debugPrint('[Dio] req body (json): $s');
      } else {
        debugPrint('[Dio] req body: omitted (declared json but body not serializable)');
      }
    } else {
      debugPrint('[Dio] req body: omitted (content-type not json)');
    }
    handler.next(options);
  }

  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) {
    debugPrint(
      '[Dio] <-- ${response.statusCode} ${response.requestOptions.uri}',
    );
    final ct = response.headers.value('content-type');
    final data = response.data;
    if (_isJsonContentType(ct)) {
      final s = _tryJsonEncode(data);
      if (s != null) {
        final preview =
            s.length > 4000 ? '${s.substring(0, 4000)}…(truncated)' : s;
        debugPrint('[Dio] resp body (json): $preview');
      } else {
        debugPrint('[Dio] resp body: omitted (not json object)');
      }
    } else if (data is Map || data is List) {
      final s = _tryJsonEncode(data);
      if (s != null) {
        debugPrint('[Dio] resp body (json-like): $s');
      }
    } else {
      debugPrint('[Dio] resp body: omitted (non-json)');
    }
    handler.next(response);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    debugPrint('[Dio] ERROR ${err.requestOptions.uri} ${err.message}');
    handler.next(err);
  }
}
