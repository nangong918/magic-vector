import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import '../service/offline_ivw_service.dart';
import '../service/vad_service.dart';
import '../service/ali_stt_service.dart';
import '../service/xfyun_chat_service.dart';

enum VoiceAgentPhase {
  initializing,
  ready,
  wakeDetectedWaitingSpeech,
  userSpeaking,
  userSpeechEnded,
  agentReplying,
  error,
}

enum WakeServiceStatus { disabled, enabledIdle, keywordDetected }

enum VadServiceStatus { disabled, noise, speech, timeout }

enum SttSendServiceStatus { disabled, sending, stoppedAfterVadEnd }

enum SttReceiveServiceStatus { noResult, receivingPartial, finalReceived }

enum AgentReplyServiceStatus { disabled, replying, finished }

class VoiceAgentViewModel extends ChangeNotifier {
  static const String wakeKeyword = '小卡小卡';
  static const int maxLogs = 300;

  final OfflineIvwService _ivwService = OfflineIvwService();
  final VadService _vadService = VadService();
  final AliSttService _sttService = AliSttService();
  final XfYunChatService _chatService = XfYunChatService();

  final List<String> _logs = <String>[];
  List<String> get logs => List<String>.unmodifiable(_logs);

  StreamSubscription<OfflineIvwEvent>? _ivwSub;
  StreamSubscription<VadEvent>? _vadSub;
  StreamSubscription<AliSttEvent>? _sttSub;
  Timer? _sttFinalTimeout;
  Timer? _vadDelayedStartTimer;
  Timer? _vadSpeechTimeoutTimer;

  String _systemPrompt = '';
  String _latestPartialStt = '';
  String _finalSttText = '';

  VoiceAgentPhase phase = VoiceAgentPhase.initializing;
  WakeServiceStatus wakeStatus = WakeServiceStatus.disabled;
  VadServiceStatus vadStatus = VadServiceStatus.disabled;
  SttSendServiceStatus sttSendStatus = SttSendServiceStatus.disabled;
  SttReceiveServiceStatus sttReceiveStatus = SttReceiveServiceStatus.noResult;
  AgentReplyServiceStatus agentReplyStatus = AgentReplyServiceStatus.disabled;

  bool _disposed = false;
  bool _recordPermissionGranted = false;
  bool _ivwAuthPassed = false;
  bool _wakeListening = false;
  bool _vadRunning = false;
  bool _vadTransitioning = false;
  bool _vadSpeechStarted = false;
  bool _speechEndHandled = false;
  bool _sttRunning = false;
  bool _sttStopRequested = false;
  bool _sttFinalReceived = false;
  bool _agentCallTriggered = false;

  Future<void> initialize() async {
    _ivwSub = _ivwService.events.listen(_handleIvwEvent);
    _vadSub = _vadService.events.listen(_handleVadEvent);
    _sttSub = _sttService.events.listen(_handleSttEvent);

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
    }
  }

  Future<void> close() async {
    _disposed = true;
    _sttFinalTimeout?.cancel();
    _vadDelayedStartTimer?.cancel();
    _vadSpeechTimeoutTimer?.cancel();
    await _ivwSub?.cancel();
    await _vadSub?.cancel();
    await _sttSub?.cancel();
    await _ivwService.release();
    await _ivwService.dispose();
    await _vadService.release();
    await _vadService.dispose();
    await _sttService.dispose();
    super.dispose();
  }

  String buildServiceStatusText() {
    return '唤醒功能：${_wakeStatusText(wakeStatus)}\n'
        'VAD功能：${_vadStatusText(vadStatus)}\n'
        'STT发送：${_sttSendStatusText(sttSendStatus)}\n'
        'STT接收：${_sttReceiveStatusText(sttReceiveStatus)}\n'
        'Agent回复：${_agentReplyStatusText(agentReplyStatus)}';
  }

  Future<void> _tryEnterReady() async {
    if (phase == VoiceAgentPhase.error || _disposed) {
      return;
    }
    if (!_recordPermissionGranted || !_ivwAuthPassed) {
      return;
    }
    _setPhase(VoiceAgentPhase.ready);
    _setWakeStatus(WakeServiceStatus.enabledIdle);
    _setVadStatus(VadServiceStatus.disabled);
    _setSttSendStatus(SttSendServiceStatus.disabled);
    _setSttReceiveStatus(SttReceiveServiceStatus.noResult);
    _setAgentReplyStatus(AgentReplyServiceStatus.disabled);
    _appendLog('就绪');
    await _startWakeListeningIfNeeded();
  }

  Future<void> _startWakeListeningIfNeeded() async {
    if (phase != VoiceAgentPhase.ready || _wakeListening) {
      return;
    }
    try {
      await _ivwService.startRecordWake(keyword: wakeKeyword);
      _wakeListening = true;
      _setWakeStatus(WakeServiceStatus.enabledIdle);
      _appendLog('开始唤醒词监听: $wakeKeyword');
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
      // best effort
    } finally {
      _wakeListening = false;
      _setWakeStatus(WakeServiceStatus.disabled);
    }
  }

  Future<void> _startWakeupSession() async {
    if (phase != VoiceAgentPhase.ready) {
      return;
    }
    _appendLog('唤醒');
    _resetRoundFlags();
    _setWakeStatus(WakeServiceStatus.keywordDetected);
    _setVadStatus(VadServiceStatus.disabled);
    _setSttSendStatus(SttSendServiceStatus.sending);
    _setSttReceiveStatus(SttReceiveServiceStatus.noResult);
    _setAgentReplyStatus(AgentReplyServiceStatus.disabled);

    try {
      await _stopWakeListeningIfNeeded();
      await _sttService.start();
      _sttRunning = true;
      _setPhase(VoiceAgentPhase.wakeDetectedWaitingSpeech);
      _appendLog('STT已启动，0~2秒不启用VAD检测');
      _scheduleVadStartAfterDelay();
    } catch (e) {
      _enterError('唤醒后流程启动失败: $e');
    }
  }

  void _scheduleVadStartAfterDelay() {
    _vadDelayedStartTimer?.cancel();
    _vadSpeechTimeoutTimer?.cancel();
    _vadDelayedStartTimer = Timer(const Duration(seconds: 2), () async {
      if (_disposed ||
          phase != VoiceAgentPhase.wakeDetectedWaitingSpeech ||
          _speechEndHandled) {
        return;
      }
      try {
        await _startSileroVad();
        _appendLog('VAD已启动，开始检测说话状态（2秒窗口）');
        _setVadStatus(VadServiceStatus.noise);
        _vadSpeechTimeoutTimer = Timer(const Duration(seconds: 2), () {
          if (_disposed ||
              phase != VoiceAgentPhase.wakeDetectedWaitingSpeech ||
              _vadSpeechStarted ||
              _speechEndHandled) {
            return;
          }
          _setVadStatus(VadServiceStatus.timeout);
          _appendLog('VAD超时：未检测到开始说话，按0~2秒已说完处理');
          _onSpeechEndDetected();
        });
      } catch (e) {
        _enterError('延迟启动VAD失败: $e');
      }
    });
  }

  Future<void> _startSileroVad() async {
    if (_vadTransitioning) {
      _appendLog('VAD状态切换中，跳过本次启动');
      return;
    }
    _vadTransitioning = true;
    try {
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
      await _vadService.stopVad(engine: VadEngine.silero);
      await Future<void>.delayed(const Duration(milliseconds: 80));
      await _vadService.startVad(
        engine: VadEngine.silero,
        sampleRate: sampleRate,
        frameSize: frameSize,
        mode: mode,
      );
      _vadRunning = true;
      _setVadStatus(VadServiceStatus.noise);
      _appendLog(
        'Silero参数: sampleRate=$sampleRate, frameSize=$frameSize, mode=$mode',
      );
    } finally {
      _vadTransitioning = false;
    }
  }

  Future<void> _stopSileroVad({String? logWhenStopped}) async {
    if (_vadTransitioning) {
      return;
    }
    _vadTransitioning = true;
    try {
      await _vadService.stopVad(engine: VadEngine.silero);
      _vadRunning = false;
      _setVadStatus(VadServiceStatus.disabled);
      if (logWhenStopped != null) {
        _appendLog(logWhenStopped);
      }
    } finally {
      _vadTransitioning = false;
    }
  }

  Future<void> _onSpeechEndDetected() async {
    if (_speechEndHandled) {
      return;
    }
    if (phase != VoiceAgentPhase.wakeDetectedWaitingSpeech &&
        phase != VoiceAgentPhase.userSpeaking) {
      return;
    }
    _speechEndHandled = true;
    _vadDelayedStartTimer?.cancel();
    _vadSpeechTimeoutTimer?.cancel();
    _setPhase(VoiceAgentPhase.userSpeechEnded);
    _appendLog('检测到用户说话结束');

    try {
      if (_vadRunning) {
        await _stopSileroVad(logWhenStopped: 'VAD已关闭（说话结束）');
      }
      _sttStopRequested = true;
      _setSttSendStatus(SttSendServiceStatus.stoppedAfterVadEnd);
      if (_sttRunning) {
        await _sttService.stop();
        _appendLog('已停止向远端STT传输音频');
      }

      _sttFinalTimeout?.cancel();
      _sttFinalTimeout = Timer(const Duration(seconds: 6), () {
        if (phase == VoiceAgentPhase.userSpeechEnded) {
          _appendLog('等待STT最终结果超时，使用当前结果继续');
          _tryCallAgentAfterSttCompleted(force: true);
        }
      });
    } catch (e) {
      _enterError('结束说话流程失败: $e');
    }
  }

  Future<void> _tryCallAgentAfterSttCompleted({required bool force}) async {
    if (_agentCallTriggered || phase != VoiceAgentPhase.userSpeechEnded) {
      return;
    }
    if (!force && !_sttFinalReceived) {
      return;
    }
    final text = _finalSttText.trim().isNotEmpty
        ? _finalSttText.trim()
        : _latestPartialStt.trim();
    if (text.isEmpty) {
      _appendLog('异常: STT未返回有效结果');
      // STT异常之后应该解除唤醒限制
      await _recoverAfterSttNoResult();
      return;
    }
    _agentCallTriggered = true;
    await _callAgentWithText(text);
  }

  Future<void> _callAgentWithText(String userText) async {
    if (phase == VoiceAgentPhase.error || _disposed) {
      return;
    }
    _sttFinalTimeout?.cancel();
    _setPhase(VoiceAgentPhase.agentReplying);
    _setWakeStatus(WakeServiceStatus.disabled);
    _setVadStatus(VadServiceStatus.disabled);
    _setSttSendStatus(SttSendServiceStatus.disabled);
    _setAgentReplyStatus(AgentReplyServiceStatus.replying);
    _appendLog('开始调用Agent: $userText');

    try {
      await _chatService.sendChat(
        systemPrompt: _systemPrompt,
        history: const <Map<String, String>>[],
        userMessage: userText,
        onDelta: (String deltaText) {
          if (deltaText.isEmpty) {
            return;
          }
          _appendLog('AI回复流: $deltaText');
        },
        onDone: () {
          _appendLog('AI回复完毕');
        },
      );
      if (phase == VoiceAgentPhase.error || _disposed) {
        return;
      }
      _setAgentReplyStatus(AgentReplyServiceStatus.finished);
      _resetRoundFlags();
      await _tryEnterReady();
    } catch (e) {
      _enterError('Agent调用失败: $e');
    }
  }

  Future<void> _stopAllFeatures() async {
    _sttFinalTimeout?.cancel();
    _vadDelayedStartTimer?.cancel();
    _vadSpeechTimeoutTimer?.cancel();
    try {
      await _stopWakeListeningIfNeeded();
    } catch (_) {}
    try {
      await _stopSileroVad();
    } catch (_) {}
    try {
      await _sttService.stop();
    } catch (_) {}
    _sttRunning = false;
    _setSttSendStatus(SttSendServiceStatus.disabled);
  }

  void _resetRoundFlags() {
    _vadDelayedStartTimer?.cancel();
    _vadSpeechTimeoutTimer?.cancel();
    _latestPartialStt = '';
    _finalSttText = '';
    _vadSpeechStarted = false;
    _speechEndHandled = false;
    _sttStopRequested = false;
    _sttFinalReceived = false;
    _agentCallTriggered = false;
    _sttRunning = false;
  }

  void _enterError(String message) {
    _appendLog('异常: $message');
    _setPhase(VoiceAgentPhase.error);
    _setWakeStatus(WakeServiceStatus.disabled);
    _setVadStatus(VadServiceStatus.disabled);
    _setSttSendStatus(SttSendServiceStatus.disabled);
    _setAgentReplyStatus(AgentReplyServiceStatus.disabled);
    _stopAllFeatures();
    _recoverWakeAfterError();
  }

  Future<void> _recoverWakeAfterError() async {
    if (_disposed) {
      return;
    }
    await Future<void>.delayed(const Duration(milliseconds: 500));
    if (_disposed) {
      return;
    }
    if (_recordPermissionGranted && _ivwAuthPassed) {
      _appendLog('异常后自动恢复唤醒监听');
      await _tryEnterReady();
    }
  }

  Future<void> _recoverAfterSttNoResult() async {
    if (_disposed) {
      return;
    }
    _setSttSendStatus(SttSendServiceStatus.disabled);
    _setSttReceiveStatus(SttReceiveServiceStatus.noResult);
    _setVadStatus(VadServiceStatus.disabled);
    _setAgentReplyStatus(AgentReplyServiceStatus.disabled);
    _resetRoundFlags();
    await _tryEnterReady();
  }

  void _handleIvwEvent(OfflineIvwEvent event) {
    if (_disposed) {
      return;
    }
    switch (event.type) {
      case OfflineIvwEventType.auth:
        _ivwAuthPassed = event.raw['code'] == 0;
        if (_ivwAuthPassed) {
          _appendLog('离线唤醒认证成功');
          _tryEnterReady();
        } else {
          _enterError('离线唤醒认证失败: ${event.message}');
        }
        break;
      case OfflineIvwEventType.wakeup:
        if (phase != VoiceAgentPhase.ready) {
          _appendLog('收到唤醒事件，但当前阶段不允许处理: $phase');
          break;
        }
        _startWakeupSession();
        break;
      case OfflineIvwEventType.error:
        _enterError('离线唤醒异常: ${event.message}');
        break;
      case OfflineIvwEventType.log:
      case OfflineIvwEventType.state:
        _appendLog(event.message);
        break;
      case OfflineIvwEventType.db:
        if (phase != VoiceAgentPhase.ready) {
          _appendLog(event.message);
        }
        break;
    }
  }

  void _handleVadEvent(VadEvent event) {
    if (_disposed || event.engine != VadEngine.silero) {
      return;
    }
    if (event.type == VadEventType.error) {
      _enterError('VAD异常: ${event.message}');
      return;
    }
    if (event.type == VadEventType.state && event.running != null) {
      _vadRunning = event.running!;
    }
    if (phase != VoiceAgentPhase.wakeDetectedWaitingSpeech &&
        phase != VoiceAgentPhase.userSpeaking) {
      return;
    }
    _appendLog(
      'VAD事件: type=${event.type.name}, state=${event.raw['state'] ?? '-'}, msg=${event.message}',
    );

    if (_isSpeechEnd(event)) {
      _appendLog('VAD检测到结束事件，准备收尾');
      _onSpeechEndDetected();
      return;
    }
    if (!_vadSpeechStarted && _isSpeechStart(event)) {
      _vadSpeechStarted = true;
      _vadSpeechTimeoutTimer?.cancel();
      _setPhase(VoiceAgentPhase.userSpeaking);
      _setVadStatus(VadServiceStatus.speech);
      _appendLog('VAD检测到用户开始说话');
    } else if (!_vadSpeechStarted) {
      _setVadStatus(VadServiceStatus.noise);
    }
  }

  void _handleSttEvent(AliSttEvent event) {
    if (_disposed) {
      return;
    }
    switch (event.type) {
      case AliSttEventType.started:
        _sttRunning = true;
        _setSttSendStatus(SttSendServiceStatus.sending);
        _appendLog('远端STT启动');
        break;
      case AliSttEventType.partial:
        _latestPartialStt = event.text ?? '';
        _setSttReceiveStatus(SttReceiveServiceStatus.receivingPartial);
        _appendLog('远端STT结果: ${event.text ?? ''}');
        break;
      case AliSttEventType.finalResult:
        _finalSttText = event.text ?? '';
        _sttFinalReceived = true;
        _setSttReceiveStatus(SttReceiveServiceStatus.finalReceived);
        _appendLog('远端STT最终结果: ${event.text ?? ''}');
        if (phase == VoiceAgentPhase.wakeDetectedWaitingSpeech ||
            phase == VoiceAgentPhase.userSpeaking) {
          _appendLog('收到最终结果，推进到说话结束态');
          _onSpeechEndDetected();
        } else if (phase == VoiceAgentPhase.userSpeechEnded && _sttStopRequested) {
          _tryCallAgentAfterSttCompleted(force: false);
        }
        break;
      case AliSttEventType.stopped:
        _sttRunning = false;
        _setSttSendStatus(SttSendServiceStatus.stoppedAfterVadEnd);
        _appendLog('远端STT停止');
        break;
      case AliSttEventType.error:
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

    if (speech is bool && speech == false) return true;
    if (speaking is bool && speaking == false) return true;
    if (voice is bool && voice == false) return true;
    if (stateText.contains('end') ||
        stateText.contains('stop') ||
        stateText.contains('silence') ||
        stateText.contains('idle')) {
      return true;
    }
    if (messageText.contains('说话结束') ||
        messageText.contains('停止说话') ||
        messageText.contains('speech end') ||
        messageText.contains('stop_speech')) {
      return true;
    }
    return rawText.contains('speech=false') ||
        rawText.contains('speaking=false') ||
        rawText.contains('voice=false');
  }

  bool _isSpeechStart(VadEvent event) {
    final dynamic speech = event.raw['speech'];
    final dynamic speaking = event.raw['speaking'];
    final dynamic voice = event.raw['voice'];
    final stateText = (event.raw['state'] ?? '').toString().toLowerCase();
    final messageText = event.message.toLowerCase();
    final rawText = event.raw.toString().toLowerCase();

    if (speech is bool && speech == true) return true;
    if (speaking is bool && speaking == true) return true;
    if (voice is bool && voice == true) return true;
    if (stateText.contains('start') || stateText.contains('speech_start')) {
      return true;
    }
    if (messageText.contains('开始说话') ||
        messageText.contains('检测到开始说话') ||
        messageText.contains('start_speech')) {
      return true;
    }
    return rawText.contains('speech=true') ||
        rawText.contains('speaking=true') ||
        rawText.contains('voice=true');
  }

  void _appendLog(String message) {
    if (_disposed || message.trim().isEmpty) {
      return;
    }
    final now = DateTime.now();
    final time =
        '${now.hour.toString().padLeft(2, '0')}:${now.minute.toString().padLeft(2, '0')}:${now.second.toString().padLeft(2, '0')}';
    _logs.add('[$time] $message');
    if (_logs.length > maxLogs) {
      _logs.removeRange(0, _logs.length - maxLogs);
    }
    _notify();
  }

  void _setPhase(VoiceAgentPhase value) {
    phase = value;
    _notify();
  }

  void _setWakeStatus(WakeServiceStatus value) {
    wakeStatus = value;
    _notify();
  }

  void _setVadStatus(VadServiceStatus value) {
    vadStatus = value;
    _notify();
  }

  void _setSttSendStatus(SttSendServiceStatus value) {
    sttSendStatus = value;
    _notify();
  }

  void _setSttReceiveStatus(SttReceiveServiceStatus value) {
    sttReceiveStatus = value;
    _notify();
  }

  void _setAgentReplyStatus(AgentReplyServiceStatus value) {
    agentReplyStatus = value;
    _notify();
  }

  void _notify() {
    if (!_disposed) {
      notifyListeners();
    }
  }

  String _wakeStatusText(WakeServiceStatus status) {
    switch (status) {
      case WakeServiceStatus.disabled:
        return '禁用';
      case WakeServiceStatus.enabledIdle:
        return '启用未被唤醒';
      case WakeServiceStatus.keywordDetected:
        return '启用检测到关键词';
    }
  }

  String _vadStatusText(VadServiceStatus status) {
    switch (status) {
      case VadServiceStatus.disabled:
        return '禁用';
      case VadServiceStatus.noise:
        return '检测到噪音';
      case VadServiceStatus.speech:
        return '检测到说话';
      case VadServiceStatus.timeout:
        return 'VAD超时';
    }
  }

  String _sttSendStatusText(SttSendServiceStatus status) {
    switch (status) {
      case SttSendServiceStatus.disabled:
        return '禁用';
      case SttSendServiceStatus.sending:
        return '正在发送';
      case SttSendServiceStatus.stoppedAfterVadEnd:
        return 'VAD结束并结束STT发送';
    }
  }

  String _sttReceiveStatusText(SttReceiveServiceStatus status) {
    switch (status) {
      case SttReceiveServiceStatus.noResult:
        return '无结果';
      case SttReceiveServiceStatus.receivingPartial:
        return '正在接收中间结果';
      case SttReceiveServiceStatus.finalReceived:
        return '已经接收最终结果';
    }
  }

  String _agentReplyStatusText(AgentReplyServiceStatus status) {
    switch (status) {
      case AgentReplyServiceStatus.disabled:
        return '禁用';
      case AgentReplyServiceStatus.replying:
        return '正在回复';
      case AgentReplyServiceStatus.finished:
        return '回复完成';
    }
  }

  String? _pick(List<String> values, String preferred) {
    if (values.isEmpty) {
      return null;
    }
    return values.contains(preferred) ? preferred : values.first;
  }
}
