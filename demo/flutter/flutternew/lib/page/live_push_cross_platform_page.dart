import 'dart:io';

import 'package:flutter/material.dart';
import 'package:rtmp_broadcaster/camera.dart';

class LivePushCrossPlatformPage extends StatefulWidget {
  const LivePushCrossPlatformPage({super.key});

  @override
  State<LivePushCrossPlatformPage> createState() => _LivePushCrossPlatformPageState();
}

class _LivePushCrossPlatformPageState extends State<LivePushCrossPlatformPage> {
  final TextEditingController _urlController = TextEditingController(
    text: 'rtmp://192.168.1.3:1935/stream/live_cross',
  );

  CameraController? _cameraController;
  List<CameraDescription> _cameras = const [];
  bool _initializing = true;
  String _status = '初始化中...';

  bool get _isStreaming => _cameraController?.value.isStreamingVideoRtmp ?? false;
  bool get _isReady => _cameraController?.value.isInitialized ?? false;

  @override
  void initState() {
    super.initState();
    _initCamera();
  }

  Future<void> _initCamera([CameraDescription? description]) async {
    if (!Platform.isAndroid && !Platform.isIOS) {
      setState(() {
        _initializing = false;
        _status = '当前平台不支持推流（仅 Android/iOS）';
      });
      return;
    }
    try {
      _cameras = await availableCameras();
      if (_cameras.isEmpty) {
        setState(() {
          _initializing = false;
          _status = '未检测到可用摄像头';
        });
        return;
      }
      final selected = description ??
          _cameras.firstWhere(
            (c) => c.lensDirection == CameraLensDirection.back,
            orElse: () => _cameras.first,
          );
      await _cameraController?.dispose();
      final controller = CameraController(
        selected,
        ResolutionPreset.medium,
        enableAudio: true,
        androidUseOpenGL: true,
      );
      await controller.initialize();
      if (!mounted) {
        await controller.dispose();
        return;
      }
      setState(() {
        _cameraController = controller;
        _initializing = false;
        _status = '摄像头就绪';
      });
    } catch (e) {
      setState(() {
        _initializing = false;
        _status = '摄像头初始化失败: $e';
      });
    }
  }

  Future<void> _startStreaming() async {
    final controller = _cameraController;
    if (controller == null || !_isReady) {
      setState(() {
        _status = '请先初始化摄像头';
      });
      return;
    }
    final url = _urlController.text.trim();
    if (url.isEmpty) {
      setState(() {
        _status = '请输入 RTMP 地址';
      });
      return;
    }
    try {
      await controller.startVideoStreaming(url, bitrate: 1_200 * 1024);
      if (!mounted) return;
      setState(() {
        _status = '推流中: $url';
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _status = '启动推流失败: $e';
      });
    }
  }

  Future<void> _stopStreaming() async {
    final controller = _cameraController;
    if (controller == null || !_isStreaming) {
      setState(() {
        _status = '当前未在推流';
      });
      return;
    }
    try {
      await controller.stopVideoStreaming();
      if (!mounted) return;
      setState(() {
        _status = '推流已停止';
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _status = '停止推流失败: $e';
      });
    }
  }

  Future<void> _switchCamera() async {
    final current = _cameraController?.description;
    if (current == null || _cameras.length < 2) {
      return;
    }
    final idx = _cameras.indexWhere((c) => c.name == current.name);
    final next = _cameras[(idx + 1) % _cameras.length];
    setState(() {
      _initializing = true;
      _status = '切换摄像头中...';
    });
    await _initCamera(next);
  }

  @override
  void dispose() {
    _cameraController?.dispose();
    _urlController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Live Push Demo (跨平台)')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('实现方式：rtmp_broadcaster（Dart + 平台插件），支持 Android/iOS。'),
            const SizedBox(height: 12),
            TextField(
              controller: _urlController,
              decoration: const InputDecoration(
                labelText: 'RTMP 地址',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            Expanded(
              child: Container(
                width: double.infinity,
                color: Colors.black,
                child: _initializing
                    ? const Center(child: CircularProgressIndicator())
                    : (_isReady
                        ? CameraPreview(_cameraController!)
                        : Center(
                            child: Text(
                              _status,
                              style: const TextStyle(color: Colors.white),
                            ),
                          )),
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: ElevatedButton(
                    onPressed: _isStreaming ? null : _startStreaming,
                    child: const Text('开始推流'),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: ElevatedButton(
                    onPressed: _isStreaming ? _stopStreaming : null,
                    child: const Text('停止推流'),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: OutlinedButton(
                    onPressed: _switchCamera,
                    child: const Text('切换摄像头'),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text('状态: $_status'),
          ],
        ),
      ),
    );
  }
}
