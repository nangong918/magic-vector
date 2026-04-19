import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:math';
import 'dart:typed_data';

import 'package:record/record.dart';
import 'package:web_socket_channel/io.dart';

import '../config/module_key_config.dart';

enum AliSttEventType {
  started,
  partial,
  finalResult,
  stopped,
  error,
}

class AliSttEvent {
  final AliSttEventType type;
  final String? text;
  final Object? error;

  AliSttEvent._(this.type, {this.text, this.error});

  factory AliSttEvent.started() => AliSttEvent._(AliSttEventType.started);

  factory AliSttEvent.partial(String text) =>
      AliSttEvent._(AliSttEventType.partial, text: text);

  factory AliSttEvent.finalResult(String text) =>
      AliSttEvent._(AliSttEventType.finalResult, text: text);

  factory AliSttEvent.stopped() => AliSttEvent._(AliSttEventType.stopped);

  factory AliSttEvent.error(Object error) =>
      AliSttEvent._(AliSttEventType.error, error: error);
}

class AliSttService {
  final AudioRecorder _record = AudioRecorder();
  final StreamController<AliSttEvent> _eventController =
      StreamController<AliSttEvent>.broadcast();

  IOWebSocketChannel? _channel;
  StreamSubscription? _channelSub;
  StreamSubscription<List<int>>? _recordSub;

  bool _isRecording = false;
  bool _sessionActive = false;
  bool _channelClosed = false;
  bool _disposed = false;
  bool _taskStarted = false;
  bool _finalEmitted = false;
  String? _taskId;
  Completer<void>? _taskStartedCompleter;

  AliSttKeyConfig? _sttConfig;
  final List<String> _sentenceResults = <String>[];
  String _latestCombinedText = '';

  Stream<AliSttEvent> get events => _eventController.stream;

  Future<void> start() async {
    if (_isRecording || _disposed) {
      return;
    }
    await _channelSub?.cancel();
    _channelSub = null;
    _closeChannel();

    final hasPermission = await _record.hasPermission();
    if (!hasPermission) {
      _eventController.add(AliSttEvent.error('麦克风权限未授权'));
      return;
    }

    _sttConfig ??= (await ModuleKeyConfigStore.load()).sttAli;
    _resetSession();
    _sessionActive = true;
    _isRecording = true;
    _safeEmit(AliSttEvent.started());

    try {
      await _openAndStartTask();
      await _waitTaskStarted();

      final stream = await _record.startStream(
        RecordConfig(
          encoder: AudioEncoder.pcm16bits,
          sampleRate: _sttConfig!.sampleRate,
          numChannels: 1,
        ),
      );
      _recordSub = stream.listen(
        _sendAudioFrame,
        onError: (Object error) => _safeEmit(AliSttEvent.error(error)),
      );
    } catch (e) {
      _safeEmit(AliSttEvent.error(e));
      await _cleanupAfterStartFailure();
    }
  }

  Future<void> stop() async {
    if (!_isRecording && !_sessionActive) {
      return;
    }
    _isRecording = false;
    _sessionActive = false;
    _safeEmit(AliSttEvent.stopped());

    await _recordSub?.cancel();
    _recordSub = null;
    await _record.stop();

    if (_channel == null || _channelClosed) {
      return;
    }
    if (_taskStarted) {
      _sendFinishTask();
    } else {
      _closeChannel();
    }
  }

  Future<void> dispose() async {
    _disposed = true;
    _sessionActive = false;
    _isRecording = false;
    await _recordSub?.cancel();
    await _record.stop();
    await _channelSub?.cancel();
    _closeChannel();
    if (!_eventController.isClosed) {
      await _eventController.close();
    }
  }

  Future<void> _openAndStartTask() async {
    final config = _sttConfig;
    if (config == null) {
      throw const ModuleKeyConfigException('STT_ALI 配置未初始化，请先调用 start()');
    }

    _taskId = _generateTaskId();
    _taskStartedCompleter = Completer<void>();

    _channel = IOWebSocketChannel.connect(
      Uri.parse(config.hostUrl),
      headers: <String, dynamic>{
        HttpHeaders.authorizationHeader: 'bearer ${config.apiKey}',
      },
    );

    _channelSub = _channel!.stream.listen(
      _handleSocketMessage,
      onError: (Object error) => _safeEmit(AliSttEvent.error(error)),
      onDone: () {
        _channelClosed = true;
      },
    );

    _sendRunTask();
  }

  Future<void> _waitTaskStarted() async {
    final completer = _taskStartedCompleter;
    if (completer == null) {
      return;
    }
    await completer.future.timeout(
      const Duration(seconds: 6),
      onTimeout: () {
        throw TimeoutException('阿里STT任务启动超时');
      },
    );
  }

  void _handleSocketMessage(dynamic message) {
    if (message is! String) {
      return;
    }

    Map<String, dynamic> jsonMap;
    try {
      jsonMap = jsonDecode(message) as Map<String, dynamic>;
    } catch (e) {
      _safeEmit(AliSttEvent.error('解析阿里STT返回失败: $e'));
      return;
    }

    final header = (jsonMap['header'] as Map?)?.cast<String, dynamic>() ??
        const <String, dynamic>{};
    final payload = (jsonMap['payload'] as Map?)?.cast<String, dynamic>() ??
        const <String, dynamic>{};
    final event = (header['event'] ?? '').toString();

    switch (event) {
      case 'task-started':
        _taskStarted = true;
        final completer = _taskStartedCompleter;
        if (completer != null && !completer.isCompleted) {
          completer.complete();
        }
        break;
      case 'result-generated':
        _handleResultGenerated(payload);
        break;
      case 'task-finished':
        _emitFinalIfNeeded();
        _closeChannel();
        break;
      case 'task-failed':
        final Object error = 'task-failed: code=${header['error_code'] ?? '-'} '
            'msg=${header['error_message'] ?? '-'}';
        _safeEmit(AliSttEvent.error(error));
        _closeChannel();
        break;
      default:
        if ((header['error_message'] ?? '').toString().isNotEmpty) {
          _safeEmit(
            AliSttEvent.error(
              '阿里STT错误: ${header['error_message']}',
            ),
          );
        }
    }
  }

  void _handleResultGenerated(Map<String, dynamic> payload) {
    final output = (payload['output'] as Map?)?.cast<String, dynamic>() ??
        const <String, dynamic>{};
    final sentence = (output['sentence'] as Map?)?.cast<String, dynamic>() ??
        const <String, dynamic>{};

    if (sentence['heartbeat'] == true) {
      return;
    }

    final text = (sentence['text'] ?? '').toString();
    if (text.isEmpty) {
      return;
    }

    final sentenceEnd = sentence['sentence_end'] == true;
    if (sentenceEnd) {
      _sentenceResults.add(text);
      _latestCombinedText = _sentenceResults.join(' ').trim();
      _safeEmit(AliSttEvent.partial(_latestCombinedText));
      return;
    }

    if (_sentenceResults.isEmpty) {
      _latestCombinedText = text;
    } else {
      _latestCombinedText = '${_sentenceResults.join(' ')} $text'.trim();
    }
    _safeEmit(AliSttEvent.partial(_latestCombinedText));
  }

  void _emitFinalIfNeeded() {
    if (_finalEmitted) {
      return;
    }
    _finalEmitted = true;
    final text = _latestCombinedText.trim();
    _safeEmit(AliSttEvent.finalResult(text));
  }

  void _sendAudioFrame(List<int> data) {
    if (!_isRecording || !_sessionActive || !_taskStarted) {
      return;
    }
    if (_channel == null || _channelClosed || data.isEmpty) {
      return;
    }
    try {
      _channel!.sink.add(Uint8List.fromList(data));
    } catch (_) {
      _channelClosed = true;
    }
  }

  void _sendRunTask() {
    final config = _sttConfig;
    final taskId = _taskId;
    if (config == null || taskId == null || _channel == null || _channelClosed) {
      return;
    }

    final parameters = <String, dynamic>{
      'format': config.format,
      'sample_rate': config.sampleRate,
      'disfluency_removal_enabled': config.disfluencyRemovalEnabled,
    };
    if (config.languageHints.isNotEmpty) {
      parameters['language_hints'] = config.languageHints;
    }

    final runTaskMessage = <String, dynamic>{
      'header': <String, dynamic>{
        'action': 'run-task',
        'task_id': taskId,
        'streaming': 'duplex',
      },
      'payload': <String, dynamic>{
        'task_group': 'audio',
        'task': 'asr',
        'function': 'recognition',
        'model': config.model,
        'parameters': parameters,
        'input': <String, dynamic>{},
      },
    };

    _channel!.sink.add(jsonEncode(runTaskMessage));
  }

  void _sendFinishTask() {
    final taskId = _taskId;
    if (taskId == null || _channel == null || _channelClosed) {
      return;
    }
    final finishTaskMessage = <String, dynamic>{
      'header': <String, dynamic>{
        'action': 'finish-task',
        'task_id': taskId,
        'streaming': 'duplex',
      },
      'payload': <String, dynamic>{
        'input': <String, dynamic>{},
      },
    };

    try {
      _channel!.sink.add(jsonEncode(finishTaskMessage));
    } catch (_) {
      _closeChannel();
    }
  }

  Future<void> _cleanupAfterStartFailure() async {
    _isRecording = false;
    _sessionActive = false;
    await _recordSub?.cancel();
    _recordSub = null;
    await _record.stop();
    await _channelSub?.cancel();
    _closeChannel();
  }

  void _closeChannel() {
    if (!_channelClosed) {
      try {
        _channel?.sink.close();
      } catch (_) {
        // ignore
      }
    }
    _channelClosed = true;
    _channel = null;
  }

  void _resetSession() {
    _channelClosed = false;
    _taskStarted = false;
    _finalEmitted = false;
    _taskId = null;
    _taskStartedCompleter = null;
    _sentenceResults.clear();
    _latestCombinedText = '';
  }

  String _generateTaskId() {
    const String chars = '0123456789abcdef';
    final Random random = Random.secure();
    return List<String>.generate(
      32,
      (_) => chars[random.nextInt(chars.length)],
    ).join();
  }

  void _safeEmit(AliSttEvent event) {
    if (_disposed || _eventController.isClosed) {
      return;
    }
    _eventController.add(event);
  }
}
