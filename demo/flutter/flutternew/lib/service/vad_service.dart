import 'dart:async';
import 'dart:io';

import 'package:flutter/services.dart';

enum VadEngine {
  webrtc,
  silero,
  yamnet,
}

enum VadEventType {
  log,
  db,
  state,
  error,
  other,
}

class VadEvent {
  final VadEngine engine;
  final VadEventType type;
  final String message;
  final bool? running;
  final int? db;
  final Map<String, dynamic> raw;

  const VadEvent({
    required this.engine,
    required this.type,
    required this.message,
    this.running,
    this.db,
    required this.raw,
  });
}

class VadOptions {
  final List<String> sampleRates;
  final List<String> frameSizes;
  final List<String> modes;

  const VadOptions({
    required this.sampleRates,
    required this.frameSizes,
    required this.modes,
  });
}

class VadService {
  static const MethodChannel _methodChannel =
      MethodChannel('com.demo.flutternew/vad');
  static const EventChannel _eventChannel =
      EventChannel('com.demo.flutternew/vad_event');

  final StreamController<VadEvent> _eventController =
      StreamController<VadEvent>.broadcast();
  StreamSubscription<dynamic>? _eventSub;

  Stream<VadEvent> get events => _eventController.stream;

  Future<void> init() async {
    if (!Platform.isAndroid) {
      _eventController.add(
        const VadEvent(
          engine: VadEngine.webrtc,
          type: VadEventType.error,
          message: 'VAD目前仅支持 Android',
          raw: {},
        ),
      );
      return;
    }
    _bindEventsIfNeeded();
  }

  Future<bool> requestRecordPermission() async {
    final bool granted =
        await _methodChannel.invokeMethod<bool>('requestRecordPermission') ??
            false;
    return granted;
  }

  Future<List<VadEngine>> getEngines() async {
    final List<dynamic> result =
        await _methodChannel.invokeMethod<List<dynamic>>('getEngines') ??
            const <dynamic>[];
    return result
        .map((dynamic e) => _parseEngine(e.toString()))
        .toSet()
        .toList();
  }

  Future<VadOptions> getOptions({
    required VadEngine engine,
    String? sampleRate,
  }) async {
    final Map<dynamic, dynamic> result =
        await _methodChannel.invokeMethod<Map<dynamic, dynamic>>(
              'getOptions',
              <String, dynamic>{
                'engine': _engineName(engine),
                'sampleRate': sampleRate ?? '',
              },
            ) ??
            const <dynamic, dynamic>{};

    List<String> toStringList(dynamic raw) {
      if (raw is List) {
        return raw.map((dynamic e) => e.toString()).toList();
      }
      return const <String>[];
    }

    return VadOptions(
      sampleRates: toStringList(result['sampleRates']),
      frameSizes: toStringList(result['frameSizes']),
      modes: toStringList(result['modes']),
    );
  }

  Future<void> startVad({
    required VadEngine engine,
    required String sampleRate,
    required String frameSize,
    required String mode,
  }) async {
    await _methodChannel.invokeMethod('startVad', <String, dynamic>{
      'engine': _engineName(engine),
      'sampleRate': sampleRate,
      'frameSize': frameSize,
      'mode': mode,
    });
  }

  Future<void> stopVad({required VadEngine engine}) async {
    await _methodChannel.invokeMethod('stopVad', <String, dynamic>{
      'engine': _engineName(engine),
    });
  }

  Future<void> release() async {
    await _methodChannel.invokeMethod('releaseVad');
  }

  Future<void> dispose() async {
    await _eventSub?.cancel();
    await _eventController.close();
  }

  void _bindEventsIfNeeded() {
    if (_eventSub != null) {
      return;
    }
    _eventSub = _eventChannel.receiveBroadcastStream().listen((dynamic event) {
      if (event is! Map) {
        return;
      }
      final raw = Map<String, dynamic>.from(event);
      final type = _parseType(raw['type']?.toString() ?? '');
      final engine = _parseEngine(raw['engine']?.toString() ?? 'webrtc');
      final message = raw['message']?.toString() ?? '';
      final db = raw['db'] is int ? raw['db'] as int : null;
      final running = raw['running'] is bool ? raw['running'] as bool : null;
      _eventController.add(
        VadEvent(
          engine: engine,
          type: type,
          message: message,
          db: db,
          running: running,
          raw: raw,
        ),
      );
    }, onError: (Object error) {
      _eventController.add(
        VadEvent(
          engine: VadEngine.webrtc,
          type: VadEventType.error,
          message: error.toString(),
          raw: const {},
        ),
      );
    });
  }

  VadEngine _parseEngine(String raw) {
    switch (raw) {
      case 'silero':
        return VadEngine.silero;
      case 'yamnet':
        return VadEngine.yamnet;
      case 'webrtc':
      default:
        return VadEngine.webrtc;
    }
  }

  VadEventType _parseType(String rawType) {
    switch (rawType) {
      case 'db':
        return VadEventType.db;
      case 'state':
        return VadEventType.state;
      case 'error':
        return VadEventType.error;
      case 'log':
        return VadEventType.log;
      default:
        return VadEventType.other;
    }
  }

  String _engineName(VadEngine engine) {
    switch (engine) {
      case VadEngine.silero:
        return 'silero';
      case VadEngine.yamnet:
        return 'yamnet';
      case VadEngine.webrtc:
        return 'webrtc';
    }
  }
}
