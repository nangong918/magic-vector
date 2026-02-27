import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:crypto/crypto.dart';

import '../config/module_key_config.dart';

class XfYunChatService {
  /// 发送聊天消息到讯飞云服务
  /// 主要功能：
  /// 1. 构建带认证信息的WebSocket URL
  /// 2. 建立WebSocket连接
  /// 3. 设置连接监听器，处理服务器响应
  /// 4. 构建请求体
  /// 5. 发送请求到服务器
  /// 6. 等待消息处理完成
  /// 7. 关闭WebSocket连接
  Future<void> sendChat({
    required String systemPrompt, // 系统提示词，用于指导AI的行为
    required List<Map<String, String>> history, // 聊天历史记录
    required String userMessage, // 用户消息
    required void Function(String deltaText) onDelta, // 处理AI回复的增量文本的回调
    required void Function() onDone, // 消息处理完成的回调
  }) async {
    final moduleConfig = await ModuleKeyConfigStore.load();
    final llmConfig = moduleConfig.llm;
    // 构建带认证信息的WebSocket URL
    final wsUrl = _buildAuthenticatedWsUrl(llmConfig);
    // 建立WebSocket连接
    final socket = await WebSocket.connect(wsUrl);
    // 创建一个Completer，用于等待消息处理完成
    final doneCompleter = Completer<void>();

    // 设置WebSocket连接监听器
    socket.listen(
      // 处理服务器发送的事件
      (dynamic event) {
        if (event is! String) {
          return;
        }
        // 处理事件，获取状态码
        final status = _handleEvent(
          event,
          onDelta: onDelta,
        );
        // 如果状态码为2（表示处理完成）且Completer未完成，则调用onDone回调并完成Completer
        if (status == 2 && !doneCompleter.isCompleted) {
          onDone();
          doneCompleter.complete();
        }
      },
      // 处理错误
      onError: (Object error, StackTrace stackTrace) {
        if (!doneCompleter.isCompleted) {
          doneCompleter.completeError(error, stackTrace);
        }
      },
      // 处理连接关闭
      onDone: () {
        if (!doneCompleter.isCompleted) {
          doneCompleter.complete();
        }
      },
      // 发生错误时取消监听
      cancelOnError: true,
    );

    // 构建请求体
    final requestBody = _buildRequestBody(
      llmConfig: llmConfig,
      systemPrompt: systemPrompt,
      history: history,
      userMessage: userMessage,
    );
    // 发送请求到服务器
    socket.add(jsonEncode(requestBody));

    try {
      // 等待消息处理完成
      await doneCompleter.future;
    } finally {
      // 关闭WebSocket连接
      await socket.close();
    }
  }

  /// 构建请求体
  /// 主要功能：
  /// 1. 构建消息列表，包括系统提示词、历史记录和用户消息
  /// 2. 构建请求头，包括app_id和uid
  /// 3. 构建请求参数，包括聊天域、温度和最大token数
  /// 4. 构建完整的请求体
  Map<String, dynamic> _buildRequestBody({
    required XfLlmKeyConfig llmConfig,
    required String systemPrompt, // 系统提示词
    required List<Map<String, String>> history, // 聊天历史记录
    required String userMessage, // 用户消息
  }) {
    // 构建消息列表
    final List<Map<String, String>> text = <Map<String, String>>[];
    // 如果系统提示词不为空，则添加到消息列表
    if (systemPrompt.trim().isNotEmpty) {
      text.add(<String, String>{'role': 'system', 'content': systemPrompt});
    }
    // 添加历史记录到消息列表
    text.addAll(history);
    // 添加用户消息到消息列表
    text.add(<String, String>{'role': 'user', 'content': userMessage});

    // 构建请求头
    final Map<String, dynamic> header = <String, dynamic>{
      'app_id': llmConfig.appId, // 应用ID
      'uid': _buildUid(), // 用户ID，使用时间戳生成
    };
    // 如果patchId不为空，则添加到请求头
    if (llmConfig.patchId.isNotEmpty) {
      header['patch_id'] = <String>[llmConfig.patchId];
    }

    // 构建完整的请求体
    return <String, dynamic>{
      'header': header, // 请求头
      'parameter': <String, dynamic>{ // 请求参数
        'chat': <String, dynamic>{
          'domain': llmConfig.domain, // 聊天域
          'temperature': 0.5, // 温度参数，控制生成文本的随机性
          'max_tokens': 4096, // 最大token数，控制生成文本的长度
        }
      },
      'payload': <String, dynamic>{ // 请求负载
        'message': <String, dynamic>{
          'text': text, // 消息列表
        }
      },
    };
  }

  /// 处理服务器发送的事件
  /// 主要功能：
  /// 1. 解析事件JSON数据
  /// 2. 检查返回码，处理错误
  /// 3. 提取增量文本并调用回调函数
  /// 4. 返回状态码
  int _handleEvent(
    String event, { // 服务器发送的事件数据
    required void Function(String deltaText) onDelta, // 处理增量文本的回调函数
  }) {
    // 解析事件JSON数据
    final Map<String, dynamic> jsonMap =
        jsonDecode(event) as Map<String, dynamic>;
    // 提取header部分
    final Map<String, dynamic> header =
        (jsonMap['header'] as Map?)?.cast<String, dynamic>() ??
            <String, dynamic>{};
    // 提取返回码
    final int code = (header['code'] as num?)?.toInt() ?? -1;
    // 检查返回码，如果不为0则抛出异常
    if (code != 0) {
      throw Exception('Chat error: code=$code sid=${header['sid']}');
    }

    // 提取payload部分
    final payload = (jsonMap['payload'] as Map?)?.cast<String, dynamic>();
    // 提取choices部分
    final choices = (payload?['choices'] as Map?)?.cast<String, dynamic>();
    // 提取text列表
    final textList = choices?['text'] as List<dynamic>? ?? <dynamic>[];

    // 遍历text列表，提取content并调用回调函数
    for (final dynamic item in textList) {
      final itemMap = (item as Map?)?.cast<String, dynamic>();
      final content = itemMap?['content'] as String? ?? '';
      if (content.isNotEmpty) {
        onDelta(content);
      }
    }
    // 返回状态码
    return (header['status'] as num?)?.toInt() ?? 0;
  }

  /// 构建带认证信息的WebSocket URL
  /// 主要功能：
  /// 1. 解析基础URL
  /// 2. 生成当前UTC时间
  /// 3. 构建签名原始字符串
  /// 4. 使用HMAC-SHA256算法生成签名
  /// 5. 构建授权字符串并进行Base64编码
  /// 6. 构建完整的认证URL
  /// 7. 将HTTP协议转换为WebSocket协议
  String _buildAuthenticatedWsUrl(XfLlmKeyConfig llmConfig) {
    // 解析基础URL
    final uri = Uri.parse(llmConfig.hostUrl);
    // 生成当前UTC时间，用于签名
    final date = HttpDate.format(DateTime.now().toUtc());
    // 构建签名原始字符串
    final signatureOrigin =
        'host: ${uri.host}\n'
        'date: $date\n'
        'GET ${uri.path} HTTP/1.1';

    // 使用HMAC-SHA256算法生成签名
    final hmacSha256 = Hmac(sha256, utf8.encode(llmConfig.apiSecret));
    final signature = base64.encode(
      hmacSha256.convert(utf8.encode(signatureOrigin)).bytes,
    );
    // 构建授权字符串
    final authorization =
        'api_key="${llmConfig.apiKey}", algorithm="hmac-sha256", headers="host date request-line", signature="$signature"';
    // 对授权字符串进行Base64编码
    final authorizationBase64 = base64.encode(utf8.encode(authorization));

    // 构建完整的认证URL
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

    // 将HTTP协议转换为WebSocket协议并返回
    return authUri
        .toString()
        .replaceFirst('https://', 'wss://')
        .replaceFirst('http://', 'ws://');
  }

  /// 构建用户ID
  /// 主要功能：
  /// 1. 使用当前时间戳作为基础
  /// 2. 如果时间戳长度超过10位，则截取最后10位
  /// 3. 否则直接使用时间戳
  String _buildUid() {
    // 使用当前时间戳作为基础
    final millis = DateTime.now().millisecondsSinceEpoch.toString();
    // 如果时间戳长度超过10位，则截取最后10位
    return millis.length > 10 ? millis.substring(millis.length - 10) : millis;
  }
}
