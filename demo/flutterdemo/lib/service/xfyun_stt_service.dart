import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:crypto/crypto.dart';
import 'package:record/record.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

import '../domain/dto/xfyun_stt_dto.dart';

enum XfIatEventType {
  started,
  partial,
  finalResult,
  stopped,
  error,
}

class XfIatEvent {
  final XfIatEventType type;
  final String? text;
  final Object? error;

  XfIatEvent._(this.type, {this.text, this.error});

  factory XfIatEvent.started() => XfIatEvent._(XfIatEventType.started);

  factory XfIatEvent.partial(String text) =>
      XfIatEvent._(XfIatEventType.partial, text: text);

  factory XfIatEvent.finalResult(String text) =>
      XfIatEvent._(XfIatEventType.finalResult, text: text);

  factory XfIatEvent.stopped() => XfIatEvent._(XfIatEventType.stopped);

  factory XfIatEvent.error(Object error) =>
      XfIatEvent._(XfIatEventType.error, error: error);
}

class XfIatService {
  static const String hostUrl = 'https://iat.xf-yun.com/v1';
  static const String appId = '4aa58263';
  static const String apiSecret = 'Y2U0OWUyYjcyOTRiYWEwMjk0YWNlMzdh';
  static const String apiKey = 'fd775746ce819ec90bc6ec2df697dbe5';

  final AudioRecorder _record = AudioRecorder();
  final StreamController<XfIatEvent> _eventController =
      StreamController<XfIatEvent>.broadcast();

  WebSocketChannel? _channel;
  StreamSubscription? _channelSub;
  StreamSubscription<List<int>>? _recordSub;
  bool _isRecording = false;
  bool _sentFirstFrame = false;
  int _seq = 0;
  final List<String> _totalWords = [];

  Stream<XfIatEvent> get events => _eventController.stream;

  Future<void> start() async {
    if (_isRecording) {
      return;
    }
    final hasPermission = await _record.hasPermission();
    if (!hasPermission) {
      _eventController.add(XfIatEvent.error('麦克风权限未授权'));
      return;
    }

    _resetSession();
    _isRecording = true;
    _eventController.add(XfIatEvent.started());

    final wsUrl = _buildWebSocketUrl();
    _channel = WebSocketChannel.connect(Uri.parse(wsUrl));
    _channelSub = _channel!.stream.listen(
      _handleSocketMessage,
      onError: (error) => _eventController.add(XfIatEvent.error(error)),
      onDone: () {},
    );

    final stream = await _record.startStream(
      const RecordConfig(
        encoder: AudioEncoder.pcm16bits,
        sampleRate: 16000,
        numChannels: 1,
      ),
    );

    _recordSub = stream.listen(
      _sendAudioFrame,
      onError: (error) => _eventController.add(XfIatEvent.error(error)),
    );
  }

  Future<void> stop() async {
    if (!_isRecording) {
      return;
    }
    _isRecording = false;
    _eventController.add(XfIatEvent.stopped());

    await _recordSub?.cancel();
    _recordSub = null;
    await _record.stop();

    _seq++;
    final frame = _buildFrame(
      status: 2,
      seq: _seq,
      audio: const <int>[],
      includeParams: false,
    );
    _channel?.sink.add(jsonEncode(frame));
  }

  Future<void> dispose() async {
    await _recordSub?.cancel();
    await _record.stop();
    await _channelSub?.cancel();
    _channel?.sink.close();
    await _eventController.close();
  }

  void _handleSocketMessage(dynamic message) {
    if (message is! String) {
      return;
    }

    final jsonMap = jsonDecode(message) as Map<String, dynamic>;
    final response = XfIatResponse.fromJson(jsonMap);
    if (response.header.code != 0) {
      _eventController.add(
        XfIatEvent.error(
          'code=${response.header.code} msg=${response.header.message ?? ''}',
        ),
      );
      return;
    }

    final result = response.payload?.result;
    if (result?.text != null && result!.text!.isNotEmpty) {
      final textPayload = XfIatText.fromBase64(result.text!);
      _applyPartialResult(textPayload);
      final current = _totalWords.join();
      _eventController.add(XfIatEvent.partial(current));
    }

    if (result?.status == 2) {
      final finalText = _totalWords.join();
      _eventController.add(XfIatEvent.finalResult(finalText));
      _channel?.sink.close();
    }
  }

  void _sendAudioFrame(List<int> data) {
    if (!_isRecording || _channel == null) {
      return;
    }
    if (data.isEmpty) {
      return;
    }
    _seq++;
    final isFirst = !_sentFirstFrame;
    final frame = _buildFrame(
      status: isFirst ? 0 : 1,
      seq: _seq,
      audio: data,
      includeParams: isFirst,
    );
    _channel!.sink.add(jsonEncode(frame));
    _sentFirstFrame = true;
  }

  Map<String, dynamic> _buildFrame({
    required int status,
    required int seq,
    required List<int> audio,
    required bool includeParams,
  }) {
    final payload = {
      'audio': {
        'encoding': 'raw',
        'sample_rate': 16000,
        'channels': 1,
        'bit_depth': 16,
        'seq': seq,
        'status': status,
        'audio': status == 2 ? '' : base64.encode(audio),
      }
    };

    final frame = <String, dynamic>{
      'header': {
        'app_id': appId,
        'status': status,
      },
      'payload': payload,
    };

    if (includeParams) {
      frame['parameter'] = {
        'iat': {
          'domain': 'slm',
          'language': 'zh_cn',
          'accent': 'mandarin',
          'eos': 6000,
          'vinfo': 1,
          'dwa': 'wpgs',
          'result': {
            'encoding': 'utf8',
            'compress': 'raw',
            'format': 'json',
          },
        }
      };
    }

    return frame;
  }

  void _applyPartialResult(XfIatText textPayload) {
    final words = textPayload.flattenWords();
    if (words.isEmpty) {
      return;
    }

    if (textPayload.pgs == 'rpl' &&
        textPayload.rg != null &&
        textPayload.rg!.length >= 2) {
      final start = textPayload.rg![0] - 1;
      final end = textPayload.rg![1] - 1;
      if (start >= 0 && start <= _totalWords.length) {
        final safeEnd = end < _totalWords.length ? end : _totalWords.length - 1;
        if (safeEnd >= start) {
          _totalWords.replaceRange(start, safeEnd + 1, words);
          return;
        }
      }
    }

    _totalWords.addAll(words);
  }

  void _resetSession() {
    _seq = 0;
    _sentFirstFrame = false;
    _totalWords.clear();
  }

  String _buildWebSocketUrl() {
    final uri = Uri.parse(hostUrl);
    final date = HttpDate.format(DateTime.now().toUtc());
    final signatureOrigin =
        'host: ${uri.host}\n' 'date: $date\n' 'GET ${uri.path} HTTP/1.1';

    final hmacSha256 = Hmac(sha256, utf8.encode(apiSecret));
    final signature = base64.encode(
      hmacSha256.convert(utf8.encode(signatureOrigin)).bytes,
    );

    final authorization =
        'api_key="$apiKey", algorithm="hmac-sha256", headers="host date request-line", signature="$signature"';
    final authorizationBase64 = base64.encode(utf8.encode(authorization));

    final authUri = Uri(
      scheme: uri.scheme,
      host: uri.host,
      path: uri.path,
      queryParameters: {
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
}
