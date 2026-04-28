import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_vlc_player/flutter_vlc_player.dart';

class LivePullCrossPlatformPage extends StatefulWidget {
  const LivePullCrossPlatformPage({super.key});

  @override
  State<LivePullCrossPlatformPage> createState() => _LivePullCrossPlatformPageState();
}

class _LivePullCrossPlatformPageState extends State<LivePullCrossPlatformPage> {
  final TextEditingController _urlController = TextEditingController(
    text: 'http://192.168.1.3:8080/hls/live/index.m3u8',
  );
  VlcPlayerController? _vlcController;
  String _status = '等待播放';

  Future<void> _startPlay() async {
    if (!Platform.isAndroid && !Platform.isIOS) {
      setState(() {
        _status = '当前平台不支持播放（仅 Android/iOS）';
      });
      return;
    }
    final url = _urlController.text.trim();
    if (url.isEmpty) {
      setState(() {
        _status = '请输入播放地址';
      });
      return;
    }
    try {
      await _vlcController?.dispose();
      final controller = VlcPlayerController.network(
        url,
        hwAcc: HwAcc.full,
        autoPlay: true,
        options: VlcPlayerOptions(),
      );
      if (!mounted) {
        await controller.dispose();
        return;
      }
      setState(() {
        _vlcController = controller;
        _status = '播放中: $url';
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _status = '启动播放失败: $e';
      });
    }
  }

  Future<void> _stopPlay() async {
    final controller = _vlcController;
    if (controller == null) {
      setState(() {
        _status = '当前没有播放中的流';
      });
      return;
    }
    try {
      await controller.stop();
      await controller.dispose();
      if (!mounted) return;
      setState(() {
        _vlcController = null;
        _status = '播放已停止';
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _status = '停止播放失败: $e';
      });
    }
  }

  @override
  void dispose() {
    _vlcController?.dispose();
    _urlController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Live Pull Demo (跨平台)')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('实现方式：flutter_vlc_player（Dart + 平台插件），支持 Android/iOS。'),
            const SizedBox(height: 12),
            TextField(
              controller: _urlController,
              decoration: const InputDecoration(
                labelText: '播放地址（RTMP/HLS）',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            Expanded(
              child: Container(
                color: Colors.black,
                width: double.infinity,
                child: _vlcController == null
                    ? const Center(
                        child: Text('点击开始播放', style: TextStyle(color: Colors.white)),
                      )
                    : VlcPlayer(
                        controller: _vlcController!,
                        aspectRatio: 16 / 9,
                        placeholder: const Center(child: CircularProgressIndicator()),
                      ),
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: ElevatedButton(
                    onPressed: _startPlay,
                    child: const Text('开始播放'),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: ElevatedButton(
                    onPressed: _stopPlay,
                    child: const Text('停止播放'),
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
