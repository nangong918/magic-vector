import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:crypto/crypto.dart';

class XfYunChatService {
  static const String hostUrl =
      'wss://maas-api.cn-huabei-1.xf-yun.com/v1.1/chat';
  static const String appId = '4aa58263';
  static const String apiSecret = 'Y2U0OWUyYjcyOTRiYWEwMjk0YWNlMzdh';
  static const String apiKey = 'fd775746ce819ec90bc6ec2df697dbe5';
  static const String patchId = '';
  static const String domain = 'xop3qwen1b7';

  Future<void> sendChat({
    required String systemPrompt,
    required List<Map<String, String>> history,
    required String userMessage,
    required void Function(String deltaText) onDelta,
    required void Function() onDone,
  }) async {
    final wsUrl = _buildAuthenticatedWsUrl();
    final socket = await WebSocket.connect(wsUrl);
    final doneCompleter = Completer<void>();

    socket.listen(
      (dynamic event) {
        if (event is! String) {
          return;
        }
        final status = _handleEvent(
          event,
          onDelta: onDelta,
        );
        if (status == 2 && !doneCompleter.isCompleted) {
          onDone();
          doneCompleter.complete();
        }
      },
      onError: (Object error, StackTrace stackTrace) {
        if (!doneCompleter.isCompleted) {
          doneCompleter.completeError(error, stackTrace);
        }
      },
      onDone: () {
        if (!doneCompleter.isCompleted) {
          doneCompleter.complete();
        }
      },
      cancelOnError: true,
    );

    final requestBody = _buildRequestBody(
      systemPrompt: systemPrompt,
      history: history,
      userMessage: userMessage,
    );
    socket.add(jsonEncode(requestBody));

    try {
      await doneCompleter.future;
    } finally {
      await socket.close();
    }
  }

  Map<String, dynamic> _buildRequestBody({
    required String systemPrompt,
    required List<Map<String, String>> history,
    required String userMessage,
  }) {
    final List<Map<String, String>> text = <Map<String, String>>[];
    if (systemPrompt.trim().isNotEmpty) {
      text.add(<String, String>{'role': 'system', 'content': systemPrompt});
    }
    text.addAll(history);
    text.add(<String, String>{'role': 'user', 'content': userMessage});

    final Map<String, dynamic> header = <String, dynamic>{
      'app_id': appId,
      'uid': _buildUid(),
    };
    if (patchId.isNotEmpty) {
      header['patch_id'] = <String>[patchId];
    }

    return <String, dynamic>{
      'header': header,
      'parameter': <String, dynamic>{
        'chat': <String, dynamic>{
          'domain': domain,
          'temperature': 0.5,
          'max_tokens': 4096,
        }
      },
      'payload': <String, dynamic>{
        'message': <String, dynamic>{
          'text': text,
        }
      },
    };
  }

  int _handleEvent(
    String event, {
    required void Function(String deltaText) onDelta,
  }) {
    final Map<String, dynamic> jsonMap =
        jsonDecode(event) as Map<String, dynamic>;
    final Map<String, dynamic> header =
        (jsonMap['header'] as Map?)?.cast<String, dynamic>() ??
            <String, dynamic>{};
    final int code = (header['code'] as num?)?.toInt() ?? -1;
    if (code != 0) {
      throw Exception('Chat error: code=$code sid=${header['sid']}');
    }

    final payload = (jsonMap['payload'] as Map?)?.cast<String, dynamic>();
    final choices = (payload?['choices'] as Map?)?.cast<String, dynamic>();
    final textList = choices?['text'] as List<dynamic>? ?? <dynamic>[];

    for (final dynamic item in textList) {
      final itemMap = (item as Map?)?.cast<String, dynamic>();
      final content = itemMap?['content'] as String? ?? '';
      if (content.isNotEmpty) {
        onDelta(content);
      }
    }
    return (header['status'] as num?)?.toInt() ?? 0;
  }

  String _buildAuthenticatedWsUrl() {
    final uri = Uri.parse(hostUrl);
    final date = HttpDate.format(DateTime.now().toUtc());
    final signatureOrigin =
        'host: ${uri.host}\n'
        'date: $date\n'
        'GET ${uri.path} HTTP/1.1';

    final hmacSha256 = Hmac(sha256, utf8.encode(apiSecret));
    final signature = base64.encode(
      hmacSha256.convert(utf8.encode(signatureOrigin)).bytes,
    );
    final authorization =
        'api_key="$apiKey", algorithm="hmac-sha256", headers="host date request-line", signature="$signature"';
    final authorizationBase64 = base64.encode(utf8.encode(authorization));

    final authUri = Uri(
      scheme: uri.scheme == 'wss' ? 'https' : 'http',
      host: uri.host,
      path: uri.path,
      queryParameters: <String, String>{
        'authorization': authorizationBase64,
        'date': date,
        'host': uri.host,
      },
    );

    return authUri
        .toString()
        .replaceFirst('https://', 'wss://')
        .replaceFirst('http://', 'ws://');
  }

  String _buildUid() {
    final millis = DateTime.now().millisecondsSinceEpoch.toString();
    return millis.length > 10 ? millis.substring(millis.length - 10) : millis;
  }
}
