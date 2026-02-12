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

  Future<void> initialize() async {
    if (_state.status != RealtimeChatStatus.notInitialized &&
        _state.status != RealtimeChatStatus.disconnected) {
      return;
    }
    _setState(const RealtimeChatState.initializing());
    try {
      _systemPrompt = await rootBundle.loadString('assets/txt/clt_agent.txt');
      _setState(const RealtimeChatState.initializedConnected());
    } catch (e) {
      _latestError = '读取系统提示词失败: $e';
      _setState(RealtimeChatState.error(_latestError));
    }
  }

  Future<void> sendMessage(String input) async {
    final message = input.trim();
    if (message.isEmpty || !canSend) {
      return;
    }

    _setState(const RealtimeChatState.recordingAndSending());
    final userMessage = _addMessage(ChatRole.user, message);
    _history.add(<String, String>{'role': 'user', 'content': userMessage.text});
    _trimHistoryIfNeeded();

    final assistantMessage = _addMessage(ChatRole.assistant, '');
    _setState(const RealtimeChatState.receiving());

    try {
      await _chatService.sendChat(
        systemPrompt: _systemPrompt,
        history: List<Map<String, String>>.from(_history),
        userMessage: message,
        onDelta: (String deltaText) {
          assistantMessage.textNotifier.value =
              '${assistantMessage.textNotifier.value}$deltaText';
        },
        onDone: () {
          _history.add(<String, String>{
            'role': 'assistant',
            'content': assistantMessage.text,
          });
          _trimHistoryIfNeeded();
        },
      );
      _setState(const RealtimeChatState.initializedConnected());
    } catch (e) {
      _latestError = '消息发送失败: $e';
      _setState(RealtimeChatState.error(_latestError));
      _setState(const RealtimeChatState.initializedConnected());
    }
  }

  ChatMessage _addMessage(ChatRole role, String text) {
    final chatMessage = ChatMessage(
      id: DateTime.now().microsecondsSinceEpoch.toString(),
      role: role,
      createdAt: DateTime.now(),
      text: text,
    );
    _messages.add(chatMessage);
    notifyListeners();
    return chatMessage;
  }

  void _trimHistoryIfNeeded() {
    int totalChars = _history.fold<int>(
      0,
      (int sum, Map<String, String> item) =>
          sum + (item['content']?.length ?? 0),
    );

    while (_history.length > 2 && totalChars > _historyMaxChars) {
      final removed = _history.removeAt(0);
      totalChars -= removed['content']?.length ?? 0;
    }
  }

  void _setState(RealtimeChatState state) {
    _state = state;
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
