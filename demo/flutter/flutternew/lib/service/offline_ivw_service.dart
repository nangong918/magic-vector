import 'dart:async';
import 'dart:io';

import 'package:flutter/services.dart';

import '../config/module_key_config.dart';

enum OfflineIvwEventType {
  log,
  db,
  wakeup,
  auth,
  state,
  error,
}

class OfflineIvwEvent {
  final OfflineIvwEventType type;
  final String message;
  final int? db;
  final Map<String, dynamic> raw;

  const OfflineIvwEvent({
    required this.type,
    required this.message,
    this.db,
    required this.raw,
  });
}

class OfflineIvwService {
  static const MethodChannel _methodChannel =
      MethodChannel('com.demo.flutternew/ivw');
  static const EventChannel _eventChannel =
      EventChannel('com.demo.flutternew/ivw_event');

  final StreamController<OfflineIvwEvent> _eventController =
      StreamController<OfflineIvwEvent>.broadcast();
  StreamSubscription<dynamic>? _eventSub;

  Stream<OfflineIvwEvent> get events => _eventController.stream;

  Future<void> init() async {
    if (!Platform.isAndroid) {
      _eventController.add(
        const OfflineIvwEvent(
          type: OfflineIvwEventType.error,
          message: '离线唤醒目前仅支持 Android',
          raw: {},
        ),
      );
      return;
    }
    _bindEventsIfNeeded();
    final config = (await ModuleKeyConfigStore.load()).offlineIvw;
    await _methodChannel.invokeMethod(
      'initSdk',
      <String, dynamic>{'config': config.toChannelArgs()},
    );
  }

  Future<void> startRecordWake({
    required String keyword,
  }) async {
    await _methodChannel.invokeMethod('startRecordWake', <String, dynamic>{
      'keyword': keyword,
    });
  }

  Future<void> stopRecordWake() async {
    await _methodChannel.invokeMethod('stopRecordWake');
  }

  Future<bool> requestRecordPermission() async {
    final bool granted =
        await _methodChannel.invokeMethod<bool>('requestRecordPermission') ??
            false;
    return granted;
  }

  Future<void> startFileWake({
    required String keyword,
    required String audioPath,
  }) async {
    await _methodChannel.invokeMethod('startFileWake', <String, dynamic>{
      'keyword': keyword,
      'audioPath': audioPath,
    });
  }

  Future<void> release() async {
    await _methodChannel.invokeMethod('releaseIvw');
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
      final message = raw['message']?.toString() ?? '';
      final db = raw['db'] is int ? raw['db'] as int : null;
      _eventController.add(
        OfflineIvwEvent(type: type, message: message, db: db, raw: raw),
      );
    }, onError: (Object error) {
      _eventController.add(
        OfflineIvwEvent(
          type: OfflineIvwEventType.error,
          message: error.toString(),
          raw: const {},
        ),
      );
    });
  }

  OfflineIvwEventType _parseType(String rawType) {
    switch (rawType) {
      case 'db':
        return OfflineIvwEventType.db;
      case 'wakeup':
        return OfflineIvwEventType.wakeup;
      case 'auth':
        return OfflineIvwEventType.auth;
      case 'state':
        return OfflineIvwEventType.state;
      case 'error':
        return OfflineIvwEventType.error;
      case 'log':
      default:
        return OfflineIvwEventType.log;
    }
  }
}
