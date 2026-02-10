import 'dart:async';

import 'package:flutter/material.dart';

import '../service/xfyun_stt_service.dart';

class XfYunSttPage extends StatefulWidget {
  const XfYunSttPage({super.key});

  @override
  State<XfYunSttPage> createState() => _XfYunSttPageState();
}

class _XfYunSttPageState extends State<XfYunSttPage> {
  final XfIatService _service = XfIatService();
  StreamSubscription<XfIatEvent>? _eventSub;

  bool _isRecording = false;
  String _resultText = '';

  @override
  void initState() {
    super.initState();
    _eventSub = _service.events.listen((event) {
      switch (event.type) {
        case XfIatEventType.started:
          setState(() => _isRecording = true);
          break;
        case XfIatEventType.partial:
        case XfIatEventType.finalResult:
          setState(() => _resultText = event.text ?? '');
          break;
        case XfIatEventType.stopped:
          setState(() => _isRecording = false);
          break;
        case XfIatEventType.error:
          setState(() => _isRecording = false);
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text(event.error.toString())),
            );
          }
          break;
      }
    });
  }

  @override
  void dispose() {
    _eventSub?.cancel();
    _service.dispose();
    super.dispose();
  }

  Future<void> _startRecording() async {
    if (_isRecording) {
      return;
    }
    await _service.start();
  }

  Future<void> _stopRecording() async {
    await _service.stop();
  }

  @override
  Widget build(BuildContext context) {
    final statusText = _isRecording ? '正在录音' : '未录音';
    final buttonColor = _isRecording ? Colors.blue : Colors.green;
    final buttonSize = _isRecording ? 96.0 : 72.0;

    return Scaffold(
      appBar: AppBar(
        title: const Text('讯飞语音识别'),
        centerTitle: true,
      ),
      body: SafeArea(
        child: Column(
          children: [
            const SizedBox(height: 16),
            Center(
              child: Text(
                statusText,
                style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
              ),
            ),
            Expanded(
              child: Center(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 24),
                  child: Text(
                    _resultText,
                    style: const TextStyle(fontSize: 12, color: Colors.black87),
                    textAlign: TextAlign.center,
                  ),
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.only(bottom: 24),
              child: GestureDetector(
                onTapDown: (_) => _startRecording(),
                onTapUp: (_) => _stopRecording(),
                onTapCancel: _stopRecording,
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 180),
                  width: buttonSize,
                  height: buttonSize,
                  decoration: BoxDecoration(
                    color: buttonColor,
                    shape: BoxShape.circle,
                    boxShadow: [
                      BoxShadow(
                        color: buttonColor.withOpacity(0.35),
                        blurRadius: 12,
                        spreadRadius: 2,
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
