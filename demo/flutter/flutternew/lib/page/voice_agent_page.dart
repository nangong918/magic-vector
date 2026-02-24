import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../service/offline_ivw_service.dart';
import '../service/vad_service.dart';
import '../service/xfyun_chat_service.dart';
import '../service/xfyun_stt_service.dart';

enum VoiceAgentPhase {
  initializing,
  ready,
  wakeDetectedWaitingSpeech,
  userSpeaking,
  userSpeechEnded,
  agentReplying,
  error,
}

class VoiceAgentPage extends StatefulWidget {
  const VoiceAgentPage({super.key});

  @override
  State<VoiceAgentPage> createState() => _VoiceAgentPageState();
}

class _VoiceAgentPageState extends State<VoiceAgentPage> {
  static const String _wakeKeyword = '小卡小卡';
  static const int _maxLogs = 300;

  final OfflineIvwService _ivwService = OfflineIvwService();
  final VadService _vadService = VadService();
  final XfIatService _sttService = XfIatService();
  final XfYunChatService _chatService = XfYunChatService();
  final ScrollController _logScrollController = ScrollController();

  StreamSubscription<OfflineIvwEvent>? _ivwSub;
  StreamSubscription<VadEvent>? _vadSub;
  StreamSubscription<XfIatEvent>? _sttSub;

  final List<String> _logs = <String>[];
  String _systemPrompt = '';
  String _latestPartialStt = '';
  String _finalSttText = '';

  VoiceAgentPhase _phase = VoiceAgentPhase.initializing;
  bool _isDisposed = false;
  bool _recordPermissionGranted = false;
  bool _ivwAuthPassed = false;
  bool _wakeListening = false;
  bool _vadRunning = false;
  bool _vadSpeechStarted = false;
  bool _speechEndHandled = false;
  bool _sttRunning = false;
  bool _sttStopRequested = false;
  bool _sttFinalReceived = false;
  bool _agentCallTriggered = false;
  Timer? _sttFinalTimeout;

  @override
  void initState() {
    super.initState();
    _ivwSub = _ivwService.events.listen(_handleIvwEvent);
    _vadSub = _vadService.events.listen(_handleVadEvent);
    _sttSub = _sttService.events.listen(_handleSttEvent);
    _initializeAgent();
  }

  @override
  void dispose() {
    _isDisposed = true;
    _sttFinalTimeout?.cancel();
    _ivwSub?.cancel();
    _vadSub?.cancel();
    _sttSub?.cancel();
    _ivwService.release();
    _ivwService.dispose();
    _vadService.release();
    _vadService.dispose();
    _sttService.dispose();
    _logScrollController.dispose();
    super.dispose();
  }

  Future<void> _initializeAgent() async {
    _setPhase(VoiceAgentPhase.initializing);
    _appendLog('初始化中...');
    try {
      _systemPrompt = await rootBundle.loadString('assets/txt/clt_agent.txt');
      _appendLog('系统提示词加载完成');

      await _vadService.init();
      await _ivwService.init();
      _appendLog('本地能力初始化完成');

      _recordPermissionGranted = await _ivwService.requestRecordPermission();
      if (!_recordPermissionGranted) {
        _enterError('录音权限未授予');
        return;
      }
      _appendLog('本地录音授权通过');
      await _tryEnterReady();
    } catch (e) {
      _enterError('初始化异常: $e');
      return;
    }
  }

  Future<void> _tryEnterReady() async {
    if (_phase == VoiceAgentPhase.error || _isDisposed) {
      return;
    }
    if (!_recordPermissionGranted || !_ivwAuthPassed) {
      return;
    }
    _setPhase(VoiceAgentPhase.ready);
    _appendLog('就绪');
    await _startWakeListeningIfNeeded();
  }

  Future<void> _startWakeListeningIfNeeded() async {
    if (_phase != VoiceAgentPhase.ready || _wakeListening) {
      return;
    }
    try {
      await _ivwService.startRecordWake(keyword: _wakeKeyword);
      _wakeListening = true;
      _appendLog('开始唤醒词监听: $_wakeKeyword');
    } catch (e) {
      _enterError('启动唤醒词监听失败: $e');
    }
  }

  Future<void> _stopWakeListeningIfNeeded() async {
    if (!_wakeListening) {
      return;
    }
    try {
      await _ivwService.stopRecordWake();
    } catch (_) {
      // ignore: stop best effort
    } finally {
      _wakeListening = false;
    }
  }

  Future<void> _startWakeupSession() async {
    if (_phase != VoiceAgentPhase.ready) {
      return;
    }
    _appendLog('唤醒');
    _latestPartialStt = '';
    _finalSttText = '';
    _vadSpeechStarted = false;
    _speechEndHandled = false;
    _sttStopRequested = false;
    _sttFinalReceived = false;
    _agentCallTriggered = false;
    _sttFinalTimeout?.cancel();

    try {
      await _stopWakeListeningIfNeeded();
      await _startSileroVad();
      await _sttService.start();
      _sttRunning = true;
      _setPhase(VoiceAgentPhase.wakeDetectedWaitingSpeech);
      _appendLog('VAD已启动，等待检测用户开始说话');
    } catch (e) {
      _enterError('唤醒后流程启动失败: $e');
    }
  }

  Future<void> _startSileroVad() async {
    final options = await _vadService.getOptions(engine: VadEngine.silero);
    final sampleRate = _pick(options.sampleRates, 'SAMPLE_RATE_8K');
    final mode = _pick(options.modes, 'NORMAL');
    final frameOptions = await _vadService.getOptions(
      engine: VadEngine.silero,
      sampleRate: sampleRate,
    );
    final frameSize = _pick(frameOptions.frameSizes, 'FRAME_SIZE_256');
    if (sampleRate == null || frameSize == null || mode == null) {
      throw Exception('Silero参数不可用');
    }
    _appendLog('Silero参数: sampleRate=$sampleRate, frameSize=$frameSize, mode=$mode');
    await _vadService.startVad(
      engine: VadEngine.silero,
      sampleRate: sampleRate,
      frameSize: frameSize,
      mode: mode,
    );
    _vadRunning = true;
  }

  Future<void> _onSpeechEndDetected() async {
    if (_speechEndHandled) {
      return;
    }
    if (_phase != VoiceAgentPhase.wakeDetectedWaitingSpeech &&
        _phase != VoiceAgentPhase.userSpeaking) {
      return;
    }
    _speechEndHandled = true;
    _setPhase(VoiceAgentPhase.userSpeechEnded);
    _appendLog('检测到用户说话结束');

    try {
      if (_vadRunning) {
        await _vadService.stopVad(engine: VadEngine.silero);
        _appendLog('VAD已关闭（说话结束）');
      }
      _vadRunning = false;

      _sttStopRequested = true;
      if (_sttRunning) {
        await _sttService.stop();
        _appendLog('已停止向远端STT传输音频');
      } else {
        _appendLog('STT当前未运行，直接等待最终结果');
      }

      _sttFinalTimeout?.cancel();
      _sttFinalTimeout = Timer(const Duration(seconds: 5), () {
        if (_phase == VoiceAgentPhase.userSpeechEnded) {
          _appendLog('等待STT结束超时，尝试使用当前结果继续');
          _tryCallAgentAfterSttCompleted(force: true);
        }
      });
    } catch (e) {
      _enterError('结束说话流程失败: $e');
    }
  }

  Future<void> _tryCallAgentAfterSttCompleted({required bool force}) async {
    if (_agentCallTriggered || _phase != VoiceAgentPhase.userSpeechEnded) {
      return;
    }
    if (!force && !_sttFinalReceived) {
      return;
    }
    final text = _finalSttText.trim().isNotEmpty
        ? _finalSttText.trim()
        : _latestPartialStt.trim();
    if (text.isEmpty) {
      _enterError('STT未返回有效结果');
      return;
    }
    _agentCallTriggered = true;
    await _callAgentWithText(text);
  }

  Future<void> _callAgentWithText(String userText) async {
    if (_phase == VoiceAgentPhase.error || _isDisposed) {
      return;
    }
    _sttFinalTimeout?.cancel();
    _setPhase(VoiceAgentPhase.agentReplying);
    _appendLog('开始调用Agent: $userText');

    final StringBuffer assistantBuffer = StringBuffer();
    try {
      await _chatService.sendChat(
        systemPrompt: _systemPrompt,
        history: const <Map<String, String>>[],
        userMessage: userText,
        onDelta: (String deltaText) {
          if (deltaText.isEmpty) {
            return;
          }
          assistantBuffer.write(deltaText);
          _appendLog('AI回复流: $deltaText');
        },
        onDone: () {
          _appendLog('AI回复完毕');
        },
      );
      if (_phase == VoiceAgentPhase.error || _isDisposed) {
        return;
      }
      _setPhase(VoiceAgentPhase.ready);
      _appendLog('就绪');
      await _startWakeListeningIfNeeded();
    } catch (e) {
      _enterError('Agent调用失败: $e');
    }
  }

  Future<void> _stopAllFeatures() async {
    _sttFinalTimeout?.cancel();
    try {
      await _stopWakeListeningIfNeeded();
    } catch (_) {}
    try {
      await _vadService.stopVad(engine: VadEngine.silero);
    } catch (_) {}
    try {
      await _sttService.stop();
    } catch (_) {}
    _vadRunning = false;
    _sttRunning = false;
  }

  void _enterError(String message) {
    _appendLog('异常: $message');
    _setPhase(VoiceAgentPhase.error);
    _stopAllFeatures();
  }

  void _setPhase(VoiceAgentPhase value) {
    if (_isDisposed) {
      return;
    }
    setState(() => _phase = value);
  }

  void _handleIvwEvent(OfflineIvwEvent event) {
    if (_isDisposed) {
      return;
    }
    switch (event.type) {
      case OfflineIvwEventType.auth:
        final code = event.raw['code'];
        _ivwAuthPassed = code == 0;
        if (_ivwAuthPassed) {
          _appendLog('离线唤醒认证成功');
          _tryEnterReady();
        } else {
          _enterError('离线唤醒认证失败: ${event.message}');
        }
        break;
      case OfflineIvwEventType.wakeup:
        if (_phase != VoiceAgentPhase.ready) {
          _appendLog('收到唤醒事件，但当前阶段不允许处理: $_phase');
          break;
        }
        _startWakeupSession();
        break;
      case OfflineIvwEventType.error:
        _enterError('离线唤醒异常: ${event.message}');
        break;
      case OfflineIvwEventType.log:
      case OfflineIvwEventType.db:
      case OfflineIvwEventType.state:
        _appendLog(event.message);
        break;
    }
  }

  void _handleVadEvent(VadEvent event) {
    if (_isDisposed) {
      return;
    }
    if (event.engine != VadEngine.silero) {
      return;
    }
    if (event.engine == VadEngine.silero &&
        (_phase == VoiceAgentPhase.wakeDetectedWaitingSpeech ||
            _phase == VoiceAgentPhase.userSpeaking)) {
      _appendLog(
        'VAD事件: type=${event.type.name}, state=${event.raw['state'] ?? '-'}, msg=${event.message}',
      );
    }
    if (event.type == VadEventType.error) {
      _enterError('VAD异常: ${event.message}');
      return;
    }
    if (_phase != VoiceAgentPhase.wakeDetectedWaitingSpeech &&
        _phase != VoiceAgentPhase.userSpeaking) {
      return;
    }
    if (_isSpeechEnd(event)) {
      _appendLog('VAD检测到结束事件，准备收尾');
      _onSpeechEndDetected();
      return;
    }
    if (!_vadSpeechStarted && _isSpeechStart(event)) {
      _vadSpeechStarted = true;
      _setPhase(VoiceAgentPhase.userSpeaking);
      _appendLog('VAD检测到用户开始说话');
      return;
    }
  }

  void _handleSttEvent(XfIatEvent event) {
    if (_isDisposed) {
      return;
    }
    switch (event.type) {
      case XfIatEventType.started:
        _appendLog('远端STT启动');
        _sttRunning = true;
        break;
      case XfIatEventType.partial:
        final text = event.text ?? '';
        _latestPartialStt = text;
        _appendLog('远端STT结果: $text');
        break;
      case XfIatEventType.finalResult:
        final text = event.text ?? '';
        _finalSttText = text;
        _sttFinalReceived = true;
        _appendLog('远端STT最终结果: $text');
        if (_phase == VoiceAgentPhase.wakeDetectedWaitingSpeech ||
            _phase == VoiceAgentPhase.userSpeaking) {
          _appendLog('收到最终结果，推进到说话结束态');
          _setPhase(VoiceAgentPhase.userSpeechEnded);
          _sttStopRequested = true;
        }
        if (_phase == VoiceAgentPhase.userSpeechEnded && _sttStopRequested) {
          _tryCallAgentAfterSttCompleted(force: false);
        }
        break;
      case XfIatEventType.stopped:
        _appendLog('远端STT停止');
        _sttRunning = false;
        break;
      case XfIatEventType.error:
        _sttRunning = false;
        _enterError('远端STT异常: ${event.error}');
        break;
    }
  }

  bool _isSpeechEnd(VadEvent event) {
    final dynamic speech = event.raw['speech'];
    final dynamic speaking = event.raw['speaking'];
    final dynamic voice = event.raw['voice'];
    final stateText = (event.raw['state'] ?? '').toString().toLowerCase();
    final messageText = event.message.toLowerCase();
    final rawText = event.raw.toString().toLowerCase();

    if (speech is bool && speech == false) {
      return true;
    }
    if (speaking is bool && speaking == false) {
      return true;
    }
    if (voice is bool && voice == false) {
      return true;
    }

    if (stateText.contains('end') ||
        stateText.contains('stop') ||
        stateText.contains('silence') ||
        stateText.contains('idle')) {
      return true;
    }
    if (messageText.contains('说话结束') ||
        messageText.contains('speech end') ||
        messageText.contains('speech_stop') ||
        messageText.contains('vad stop')) {
      return true;
    }
    if (rawText.contains('speech=false') ||
        rawText.contains('speaking=false') ||
        rawText.contains('voice=false')) {
      return true;
    }
    return false;
  }

  bool _isSpeechStart(VadEvent event) {
    final dynamic speech = event.raw['speech'];
    final dynamic speaking = event.raw['speaking'];
    final dynamic voice = event.raw['voice'];
    final stateText = (event.raw['state'] ?? '').toString().toLowerCase();
    final messageText = event.message.toLowerCase();
    final rawText = event.raw.toString().toLowerCase();

    if (speech is bool && speech == true) {
      return true;
    }
    if (speaking is bool && speaking == true) {
      return true;
    }
    if (voice is bool && voice == true) {
      return true;
    }

    if (stateText.contains('speech_start') ||
        stateText.contains('start_speech') ||
        stateText.contains('voice_start') ||
        stateText.contains('begin')) {
      return true;
    }
    if (messageText.contains('开始说话') ||
        messageText.contains('speech start') ||
        messageText.contains('speech_begin') ||
        messageText.contains('voice start')) {
      return true;
    }
    if (rawText.contains('speech=true') ||
        rawText.contains('speaking=true') ||
        rawText.contains('voice=true')) {
      return true;
    }
    return false;
  }

  String? _pick(List<String> values, String preferred) {
    if (values.isEmpty) {
      return null;
    }
    return values.contains(preferred) ? preferred : values.first;
  }

  void _appendLog(String message) {
    if (_isDisposed || message.trim().isEmpty) {
      return;
    }
    final now = DateTime.now();
    final time =
        '${now.hour.toString().padLeft(2, '0')}:${now.minute.toString().padLeft(2, '0')}:${now.second.toString().padLeft(2, '0')}';
    setState(() {
      _logs.add('[$time] $message');
      if (_logs.length > _maxLogs) {
        _logs.removeRange(0, _logs.length - _maxLogs);
      }
    });
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!_logScrollController.hasClients) {
        return;
      }
      _logScrollController.animateTo(
        _logScrollController.position.maxScrollExtent,
        duration: const Duration(milliseconds: 180),
        curve: Curves.easeOut,
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    final Color ballColor = _ballColor();
    final double targetSize = _ballSize();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Voice Agent'),
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
                      itemCount: _logs.length,
                      itemBuilder: (BuildContext context, int index) {
                        return Text(
                          _logs[index],
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
    switch (_phase) {
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
    switch (_phase) {
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
    switch (_phase) {
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
}
