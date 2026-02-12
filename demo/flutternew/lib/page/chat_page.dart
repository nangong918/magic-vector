import 'package:flutter/material.dart';

import '../viewmodel/chat_view_model.dart';

class ChatPage extends StatefulWidget {
  final String title;
  final String? description;

  const ChatPage({
    super.key,
    required this.title,
    this.description,
  });

  @override
  State<ChatPage> createState() => _ChatPageState();
}


class _ChatPageState extends State<ChatPage> {
  final ChatViewModel _viewModel = ChatViewModel();
  final TextEditingController _inputController = TextEditingController();
  final ScrollController _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    _viewModel.addListener(_onViewModelChanged);
    _viewModel.initialize();
  }

  void _onViewModelChanged() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!_scrollController.hasClients) {
        return;
      }
      _scrollController.animateTo(
        _scrollController.position.maxScrollExtent + 80,
        duration: const Duration(milliseconds: 220),
        curve: Curves.easeOut,
      );
    });
    if (mounted && _viewModel.state.status == RealtimeChatStatus.error) {
      final message = _viewModel.latestError;
      if (message.isNotEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(message)),
        );
      }
    }
  }

  @override
  void dispose() {
    _viewModel.removeListener(_onViewModelChanged);
    _viewModel.dispose();
    _inputController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> _send() async {
    final text = _inputController.text.trim();
    if (text.isEmpty || !_viewModel.canSend) {
      return;
    }
    _inputController.clear();
    setState(() {});
    await _viewModel.sendMessage(text);
    if (mounted) {
      setState(() {});
    }
  }

  String _buildStateText(RealtimeChatState state) {
    switch (state.status) {
      case RealtimeChatStatus.notInitialized:
        return '未初始化';
      case RealtimeChatStatus.initializing:
        return '初始化中...';
      case RealtimeChatStatus.initializedConnected:
        return '已连接';
      case RealtimeChatStatus.recordingAndSending:
        return '发送中...';
      case RealtimeChatStatus.receiving:
        return 'AI回复中...';
      case RealtimeChatStatus.disconnected:
        return '已断开';
      case RealtimeChatStatus.error:
        return state.message == null ? '错误' : '错误: ${state.message}';
    }
  }

  @override
  Widget build(BuildContext context) {
    final canSendNow = _viewModel.canSend && _inputController.text.trim().isNotEmpty;
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
        backgroundColor: const Color(0xFFF48FB1),
        foregroundColor: Colors.white,
      ),
      body: Column(
        children: [
          AnimatedBuilder(
            animation: _viewModel,
            builder: (BuildContext context, _) {
              final statusText = _buildStateText(_viewModel.state);
              return Container(
                width: double.infinity,
                color: Colors.pink.shade50,
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                child: Text(
                  statusText,
                  style: TextStyle(
                    fontSize: 12,
                    color: _viewModel.state.status == RealtimeChatStatus.error
                        ? Colors.red
                        : Colors.black54,
                  ),
                ),
              );
            },
          ),
          Expanded(
            child: AnimatedBuilder(
              animation: _viewModel,
              builder: (BuildContext context, _) {
                final messages = _viewModel.messages;
                return ListView.builder(
                  controller: _scrollController,
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 12),
                  itemCount: messages.length,
                  itemBuilder: (BuildContext context, int index) {
                    return ChatBubbleItem(message: messages[index]);
                  },
                );
              },
            ),
          ),
          SafeArea(
            top: false,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(12, 8, 12, 10),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Expanded(
                    child: TextField(
                      controller: _inputController,
                      maxLines: 5,
                      minLines: 1,
                      onChanged: (_) => setState(() {}),
                      decoration: InputDecoration(
                        hintText: _viewModel.isReceiving ? 'AI回复中，请稍候...' : '输入消息',
                        filled: true,
                        fillColor: Colors.grey.shade100,
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(24),
                          borderSide: BorderSide.none,
                        ),
                        contentPadding: const EdgeInsets.symmetric(
                          horizontal: 16,
                          vertical: 12,
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Material(
                    color: canSendNow ? const Color(0xFFF48FB1) : Colors.grey.shade400,
                    borderRadius: BorderRadius.circular(20),
                    child: InkWell(
                      borderRadius: BorderRadius.circular(20),
                      onTap: canSendNow ? _send : null,
                      child: const Padding(
                        padding: EdgeInsets.all(12),
                        child: Icon(Icons.send, color: Colors.white, size: 20),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class ChatBubbleItem extends StatelessWidget {
  const ChatBubbleItem({super.key, required this.message});

  final ChatMessage message;

  @override
  Widget build(BuildContext context) {
    final isUser = message.role == ChatRole.user;
    final bubbleColor = const Color(0xFFE8F5E9);
    final align = isUser ? CrossAxisAlignment.end : CrossAxisAlignment.start;

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: isUser ? MainAxisAlignment.end : MainAxisAlignment.start,
        children: [
          if (!isUser) ...[
            const CircleAvatar(
              radius: 15,
              backgroundColor: Color(0xFFF48FB1),
              child: Icon(Icons.smart_toy, color: Colors.white, size: 16),
            ),
            const SizedBox(width: 6),
          ],
          Flexible(
            child: Column(
              crossAxisAlignment: align,
              children: [
                Container(
                  constraints: BoxConstraints(
                    maxWidth: MediaQuery.of(context).size.width * 0.78,
                  ),
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  decoration: BoxDecoration(
                    color: bubbleColor,
                    borderRadius: BorderRadius.only(
                      topLeft: const Radius.circular(24),
                      topRight: const Radius.circular(24),
                      bottomLeft: Radius.circular(isUser ? 24 : 0),
                      bottomRight: Radius.circular(isUser ? 0 : 24),
                    ),
                  ),
                  child: ValueListenableBuilder<String>(
                    valueListenable: message.textNotifier,
                    builder: (context, String value, child) {
                      return Text(
                        value,
                        style: const TextStyle(
                          color: Color(0xFF263238),
                          fontSize: 16,
                          height: 1.4,
                        ),
                      );
                    },
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  _formatTime(message.createdAt),
                  style: const TextStyle(
                    fontSize: 11,
                    color: Colors.black45,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  static String _formatTime(DateTime dateTime) {
    final hh = dateTime.hour.toString().padLeft(2, '0');
    final mm = dateTime.minute.toString().padLeft(2, '0');
    return '$hh:$mm';
  }
}