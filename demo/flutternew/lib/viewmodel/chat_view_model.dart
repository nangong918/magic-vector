import 'dart:collection';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import '../service/xfyun_chat_service.dart';

enum RealtimeChatStatus {
  notInitialized,
  initializing,
  initializedConnected,
  recordingAndSending,
  receiving,
  disconnected,
  error,
}

class RealtimeChatState {
  final RealtimeChatStatus status;
  final String? message;

  const RealtimeChatState._(this.status, [this.message]);

  const RealtimeChatState.notInitialized()
    : this._(RealtimeChatStatus.notInitialized);

  const RealtimeChatState.initializing() : this._(RealtimeChatStatus.initializing);

  const RealtimeChatState.initializedConnected()
    : this._(RealtimeChatStatus.initializedConnected);

  const RealtimeChatState.recordingAndSending()
    : this._(RealtimeChatStatus.recordingAndSending);

  const RealtimeChatState.receiving() : this._(RealtimeChatStatus.receiving);

  const RealtimeChatState.disconnected() : this._(RealtimeChatStatus.disconnected);

  const RealtimeChatState.error(String message)
    : this._(RealtimeChatStatus.error, message);
}

enum ChatRole { user, assistant }

class ChatMessage {
  ChatMessage({
    required this.id,
    required this.role,
    required this.createdAt,
    required String text,
  }) : textNotifier = ValueNotifier<String>(text);

  final String id;
  final ChatRole role;
  final DateTime createdAt;
  final ValueNotifier<String> textNotifier;

  String get text => textNotifier.value;

  void dispose() {
    textNotifier.dispose();
  }
}

class ChatViewModel extends ChangeNotifier {
  ChatViewModel({XfYunChatService? chatService})
    : _chatService = chatService ?? XfYunChatService();

  static const int _historyMaxChars = 12000;

  final XfYunChatService _chatService;
  final List<ChatMessage> _messages = <ChatMessage>[];
  final List<Map<String, String>> _history = <Map<String, String>>[];

  String _systemPrompt = '';
  RealtimeChatState _state = const RealtimeChatState.notInitialized();
  String _latestError = '';
  bool _disposed = false;

  UnmodifiableListView<ChatMessage> get messages =>
      UnmodifiableListView<ChatMessage>(_messages);

  RealtimeChatState get state => _state;

  bool get canSend => _state.status == RealtimeChatStatus.initializedConnected;

  bool get isReceiving => _state.status == RealtimeChatStatus.receiving;

  String get latestError => _latestError;

  /// 初始化ViewModel
  /// 主要功能：
  /// 1. 检查当前状态是否允许初始化
  /// 2. 设置状态为初始化中
  /// 3. 加载系统提示词
  /// 4. 设置状态为已连接
  /// 5. 处理初始化过程中的错误
  Future<void> initialize() async {
    // 检查当前状态是否为未初始化或已断开，只有这两种状态可以初始化
    if (_state.status != RealtimeChatStatus.notInitialized &&
        _state.status != RealtimeChatStatus.disconnected) {
      return;
    }
    // 设置状态为初始化中
    _setState(const RealtimeChatState.initializing());
    try {
      // 加载系统提示词，用于指导AI的行为
      _systemPrompt = await rootBundle.loadString('assets/txt/clt_agent.txt');
      // 初始化成功，设置状态为已连接
      _setState(const RealtimeChatState.initializedConnected());
    } catch (e) {
      // 初始化失败，设置错误信息和状态
      _latestError = '读取系统提示词失败: $e';
      _setState(RealtimeChatState.error(_latestError));
    }
  }

  /// 发送消息
  /// 主要功能：
  /// 1. 检查消息是否为空和是否可以发送
  /// 2. 设置状态为发送中
  /// 3. 添加用户消息到消息列表和历史记录
  /// 4. 裁剪历史记录（如果需要）
  /// 5. 添加空的助手消息到消息列表
  /// 6. 设置状态为接收中
  /// 7. 调用聊天服务发送消息
  /// 8. 处理AI回复的增量文本
  /// 9. 处理消息发送完成
  /// 10. 设置状态为已连接
  /// 11. 处理消息发送过程中的错误
  Future<void> sendMessage(String input) async {
    final message = input.trim();
    // 检查消息是否为空和是否可以发送（状态是否为已连接）
    if (message.isEmpty || !canSend) {
      return;
    }

    // 设置状态为发送中
    _setState(const RealtimeChatState.recordingAndSending());
    // 添加用户消息到消息列表
    final userMessage = _addMessage(ChatRole.user, message);
    // 添加用户消息到历史记录
    _history.add(<String, String>{'role': 'user', 'content': userMessage.text});
    // 裁剪历史记录（如果超过最大长度）
    _trimHistoryIfNeeded();

    // 添加空的助手消息到消息列表，用于接收AI回复
    final assistantMessage = _addMessage(ChatRole.assistant, '');
    // 设置状态为接收中
    _setState(const RealtimeChatState.receiving());

    try {
      // 调用聊天服务发送消息
      await _chatService.sendChat(
        systemPrompt: _systemPrompt,
        history: List<Map<String, String>>.from(_history),
        userMessage: message,
        // 处理AI回复的增量文本
        onDelta: (String deltaText) {
          assistantMessage.textNotifier.value =
              '${assistantMessage.textNotifier.value}$deltaText';
        },
        // 处理消息发送完成
        onDone: () {
          // 添加助手消息到历史记录
          _history.add(<String, String>{
            'role': 'assistant',
            'content': assistantMessage.text,
          });
          // 裁剪历史记录（如果超过最大长度）
          _trimHistoryIfNeeded();
        },
      );
      // 消息发送完成，设置状态为已连接
      _setState(const RealtimeChatState.initializedConnected());
    } catch (e) {
      // 消息发送失败，设置错误信息和状态
      _latestError = '消息发送失败: $e';
      _setState(RealtimeChatState.error(_latestError));
      // 错误处理完成后，设置状态为已连接
      _setState(const RealtimeChatState.initializedConnected());
    }
  }

  /// 添加消息到消息列表
  /// 主要功能：
  /// 1. 创建新的ChatMessage对象
  /// 2. 添加到消息列表
  /// 3. 通知监听器（UI）更新
  /// 4. 返回创建的消息对象
  ChatMessage _addMessage(ChatRole role, String text) {
    // 创建新的ChatMessage对象，使用当前时间戳作为ID
    final chatMessage = ChatMessage(
      id: DateTime.now().microsecondsSinceEpoch.toString(),
      role: role,
      createdAt: DateTime.now(),
      text: text,
    );
    // 添加到消息列表
    _messages.add(chatMessage);
    // 通知监听器（UI）更新
    notifyListeners();
    // 返回创建的消息对象
    return chatMessage;
  }

  /// 裁剪历史记录（如果需要）
  /// 主要功能：
  /// 1. 计算历史记录的总字符数
  /// 2. 如果历史记录超过最大长度且长度大于2，则从开头移除消息
  /// 3. 直到总字符数小于等于最大长度或历史记录长度小于等于2
  void _trimHistoryIfNeeded() {
    // 计算历史记录的总字符数
    int totalChars = _history.fold<int>(
      0,
      (int sum, Map<String, String> item) =>
          sum + (item['content']?.length ?? 0),
    );

    // 如果历史记录超过最大长度且长度大于2，则从开头移除消息
    while (_history.length > 2 && totalChars > _historyMaxChars) {
      final removed = _history.removeAt(0);
      // 更新总字符数
      totalChars -= removed['content']?.length ?? 0;
    }
  }

  /// 设置ViewModel状态
  /// 主要功能：
  /// 1. 更新内部状态
  /// 2. 如果ViewModel未被销毁，则通知监听器（UI）更新
  void _setState(RealtimeChatState state) {
    // 更新内部状态
    _state = state;
    // 如果ViewModel未被销毁，则通知监听器（UI）更新
    if (!_disposed) {
      notifyListeners();
    }
  }

  @override
  void dispose() {
    _disposed = true;
    for (final item in _messages) {
      item.dispose();
    }
    super.dispose();
  }
}
