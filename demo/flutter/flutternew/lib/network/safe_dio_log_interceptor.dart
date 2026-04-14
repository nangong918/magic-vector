import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

/// 避免 multipart 上传在控制台打印二进制；普通请求仍打印 method、url、headers。
class SafeDioLogInterceptor extends Interceptor {
  static bool _isMultipartUploadPath(String path) =>
      path.contains('/oss/upload/batch') ||
      path.contains('/oss/file/content/update');

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    if (options.data is FormData &&
        _isMultipartUploadPath(options.uri.path)) {
      final fd = options.data as FormData;
      final fileKeys = fd.files.map((e) => e.key).join(', ');
      debugPrint(
        '[Dio] ${options.method} ${options.uri} '
        '(multipart: ${fd.fields.length} fields, files: [$fileKeys]; body not logged)',
      );
    } else {
      debugPrint('[Dio] ${options.method} ${options.uri}');
      if (options.headers.isNotEmpty) {
        debugPrint('[Dio] headers: ${options.headers}');
      }
      if (options.data != null && options.data is! FormData) {
        debugPrint('[Dio] body: ${options.data}');
      }
    }
    handler.next(options);
  }

  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) {
    debugPrint(
      '[Dio] <-- ${response.statusCode} ${response.requestOptions.uri}',
    );
    if (response.data != null &&
        response.data is Map &&
        (response.data as Map).length < 40) {
      debugPrint('[Dio] data: ${response.data}');
    }
    handler.next(response);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    debugPrint('[Dio] ERROR ${err.requestOptions.uri} ${err.message}');
    handler.next(err);
  }
}
