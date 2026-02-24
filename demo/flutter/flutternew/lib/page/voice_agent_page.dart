import 'package:flutter/material.dart';

import '../viewmodel/voice_agent_view_model.dart';

class VoiceAgentPage extends StatefulWidget {
  const VoiceAgentPage({super.key});

  @override
  State<VoiceAgentPage> createState() => _VoiceAgentPageState();
}

class _VoiceAgentPageState extends State<VoiceAgentPage> {
  final VoiceAgentViewModel _viewModel = VoiceAgentViewModel();
  final ScrollController _logScrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    _viewModel.addListener(_onViewModelChanged);
    _viewModel.initialize();
  }

  @override
  void dispose() {
    _viewModel.removeListener(_onViewModelChanged);
    _viewModel.close();
    _logScrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final Color ballColor = _ballColor();
    final double targetSize = _ballSize();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Voice Agent'),
        actions: [
          Padding(
            padding: const EdgeInsets.fromLTRB(8, 8, 12, 8),
            child: Container(
              constraints: const BoxConstraints(maxWidth: 220),
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: Colors.black.withValues(alpha: 0.65),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(
                _viewModel.buildServiceStatusText(),
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 10,
                  height: 1.25,
                ),
              ),
            ),
          ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: <Widget>[
            Expanded(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  AnimatedContainer(
                    duration: const Duration(milliseconds: 420),
                    curve: Curves.elasticOut,
                    width: targetSize,
                    height: targetSize,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: ballColor,
                      boxShadow: <BoxShadow>[
                        BoxShadow(
                          color: ballColor.withValues(alpha: 0.28),
                          blurRadius: 18,
                          spreadRadius: 4,
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    _statusText(),
                    style: const TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
            ),
            Container(
              height: 260,
              margin: const EdgeInsets.fromLTRB(12, 0, 12, 12),
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: Colors.white.withValues(alpha: 0.45),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: Colors.black.withValues(alpha: 0.12)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  const Text(
                    '控制台日志',
                    style: TextStyle(
                      color: Colors.black87,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Expanded(
                    child: ListView.builder(
                      controller: _logScrollController,
                      itemCount: _viewModel.logs.length,
                      itemBuilder: (BuildContext context, int index) {
                        return Text(
                          _viewModel.logs[index],
                          style: const TextStyle(
                            color: Colors.black87,
                            fontSize: 12,
                          ),
                        );
                      },
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Color _ballColor() {
    switch (_viewModel.phase) {
      case VoiceAgentPhase.ready:
        return Colors.green;
      case VoiceAgentPhase.wakeDetectedWaitingSpeech:
      case VoiceAgentPhase.userSpeaking:
      case VoiceAgentPhase.userSpeechEnded:
        return Colors.blue;
      case VoiceAgentPhase.agentReplying:
        return Colors.purple;
      case VoiceAgentPhase.error:
        return Colors.red;
      case VoiceAgentPhase.initializing:
        return Colors.red;
    }
  }

  double _ballSize() {
    switch (_viewModel.phase) {
      case VoiceAgentPhase.wakeDetectedWaitingSpeech:
        return 150;
      case VoiceAgentPhase.userSpeaking:
        return 180;
      case VoiceAgentPhase.userSpeechEnded:
        return 150;
      case VoiceAgentPhase.agentReplying:
        return 162;
      case VoiceAgentPhase.ready:
      case VoiceAgentPhase.error:
      case VoiceAgentPhase.initializing:
        return 150;
    }
  }

  String _statusText() {
    switch (_viewModel.phase) {
      case VoiceAgentPhase.initializing:
        return '初始化中';
      case VoiceAgentPhase.ready:
        return '就绪（仅唤醒监听中）';
      case VoiceAgentPhase.wakeDetectedWaitingSpeech:
        return '已唤醒，等待用户开始说话';
      case VoiceAgentPhase.userSpeaking:
        return '唤醒后讲话中（VAD+STT）';
      case VoiceAgentPhase.userSpeechEnded:
        return '讲话结束，等待STT最终结果';
      case VoiceAgentPhase.agentReplying:
        return 'Agent回复中';
      case VoiceAgentPhase.error:
        return '异常（功能已禁用）';
    }
  }

  void _onViewModelChanged() {
    if (!mounted) {
      return;
    }
    setState(() {});
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!_logScrollController.hasClients) {
        return;
      }
      _logScrollController.animateTo(
        _logScrollController.position.maxScrollExtent,
        duration: const Duration(milliseconds: 160),
        curve: Curves.easeOut,
      );
    });
  }
}
