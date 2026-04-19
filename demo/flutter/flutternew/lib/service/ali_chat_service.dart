import 'dart:async';
import 'dart:convert';
import 'dart:io';

import '../config/module_key_config.dart';

class AliChatService {
  Future<void> sendChat({
    required String systemPrompt,
    required List<Map<String, String>> history,
    required String userMessage,
    required void Function(String deltaText) onDelta,
    required void Function() onDone,
  }) async {
    final moduleConfig = await ModuleKeyConfigStore.load();
    final llmConfig = moduleConfig.llmAli;

    final requestBody = _buildRequestBody(
      llmConfig: llmConfig,
      systemPrompt: systemPrompt,
      history: history,
      userMessage: userMessage,
    );

    final client = HttpClient();
    bool doneNotified = false;

    void notifyDone() {
      if (doneNotified) {
        return;
      }
      doneNotified = true;
      onDone();
    }

    try {
      final request = await client.postUrl(Uri.parse(llmConfig.hostUrl));
      request.headers.set(
        HttpHeaders.authorizationHeader,
        'Bearer ${llmConfig.apiKey}',
      );
      request.headers.set(
        HttpHeaders.contentTypeHeader,
        'application/json',
      );
      request.add(utf8.encode(jsonEncode(requestBody)));

      final response = await request.close();
      if (response.statusCode < 200 || response.statusCode >= 300) {
        final errorBody = await response.transform(utf8.decoder).join();
        throw Exception(
          'Ali LLM请求失败: status=${response.statusCode}, body=$errorBody',
        );
      }

      String buffer = '';
      await for (final String chunk in response.transform(utf8.decoder)) {
        buffer += chunk;
        buffer = _consumeSseBuffer(
          buffer,
          onDelta: onDelta,
          onDone: notifyDone,
        );
      }
      final tail = buffer.trim();
      if (tail.isNotEmpty) {
        _handleSseLine(
          tail,
          onDelta: onDelta,
          onDone: notifyDone,
        );
      }

      notifyDone();
    } finally {
      client.close(force: true);
    }
  }

  Map<String, dynamic> _buildRequestBody({
    required AliLlmKeyConfig llmConfig,
    required String systemPrompt,
    required List<Map<String, String>> history,
    required String userMessage,
  }) {
    final List<Map<String, String>> messages = <Map<String, String>>[];
    if (systemPrompt.trim().isNotEmpty) {
      messages.add(<String, String>{
        'role': 'system',
        'content': systemPrompt.trim(),
      });
    }

    for (final item in history) {
      final content = (item['content'] ?? '').trim();
      if (content.isEmpty) {
        continue;
      }
      messages.add(<String, String>{
        'role': _normalizeRole(item['role']),
        'content': content,
      });
    }

    messages.add(<String, String>{
      'role': 'user',
      'content': userMessage,
    });

    return <String, dynamic>{
      'model': llmConfig.model,
      'messages': messages,
      'stream': true,
      'temperature': llmConfig.temperature,
      'max_tokens': llmConfig.maxTokens,
    };
  }

  String _normalizeRole(String? role) {
    switch ((role ?? '').toLowerCase()) {
      case 'system':
      case 'assistant':
      case 'user':
        return role!.toLowerCase();
      default:
        return 'user';
    }
  }

  String _consumeSseBuffer(
    String buffer, {
    required void Function(String deltaText) onDelta,
    required void Function() onDone,
  }) {
    while (true) {
      final int newLineIndex = buffer.indexOf('\n');
      if (newLineIndex < 0) {
        return buffer;
      }
      final String line = buffer.substring(0, newLineIndex).trim();
      buffer = buffer.substring(newLineIndex + 1);
      if (line.isEmpty) {
        continue;
      }
      _handleSseLine(line, onDelta: onDelta, onDone: onDone);
    }
  }

  void _handleSseLine(
    String line, {
    required void Function(String deltaText) onDelta,
    required void Function() onDone,
  }) {
    if (!line.startsWith('data:')) {
      return;
    }

    final String data = line.substring(5).trim();
    if (data.isEmpty) {
      return;
    }
    if (data == '[DONE]') {
      onDone();
      return;
    }

    final Map<String, dynamic> jsonMap = jsonDecode(data) as Map<String, dynamic>;
    if (jsonMap['error'] != null) {
      throw Exception('Ali LLM返回错误: ${jsonMap['error']}');
    }

    final List<dynamic> choices = jsonMap['choices'] as List<dynamic>? ?? <dynamic>[];
    if (choices.isEmpty) {
      return;
    }

    final Map<String, dynamic> firstChoice =
        (choices.first as Map?)?.cast<String, dynamic>() ?? <String, dynamic>{};
    final Map<String, dynamic> delta =
        (firstChoice['delta'] as Map?)?.cast<String, dynamic>() ?? <String, dynamic>{};
    final String deltaText = (delta['content'] ?? '').toString();
    if (deltaText.isNotEmpty) {
      onDelta(deltaText);
    }

    final String finishReason = (firstChoice['finish_reason'] ?? '').toString();
    if (finishReason.isNotEmpty && finishReason != 'null') {
      onDone();
    }
  }
}
