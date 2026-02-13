import 'dart:async';

import 'package:flutter/material.dart';

import '../service/offline_ivw_service.dart';

class OfflineIvwPage extends StatefulWidget {
  const OfflineIvwPage({super.key});

  @override
  State<OfflineIvwPage> createState() => _OfflineIvwPageState();
}

class _OfflineIvwPageState extends State<OfflineIvwPage>
    with SingleTickerProviderStateMixin {
  final OfflineIvwService _service = OfflineIvwService();
  final TextEditingController _keywordController =
      TextEditingController(text: '你好小迪');
  final TextEditingController _audioPathController = TextEditingController();
  final List<String> _logs = <String>[];

  StreamSubscription<OfflineIvwEvent>? _eventSub;
  bool _sdkReady = false;
  bool _isListening = false;
  int _db = 0;

  @override
  void initState() {
    super.initState();
    _eventSub = _service.events.listen(_handleEvent);
    _initSdk();
  }

  @override
  void dispose() {
    _eventSub?.cancel();
    _service.dispose();
    _keywordController.dispose();
    _audioPathController.dispose();
    super.dispose();
  }

  Future<void> _initSdk() async {
    try {
      await _service.init();
      if (!mounted) {
        return;
      }
      setState(() => _sdkReady = true);
      _appendLog('SDK初始化请求已发送');
    } catch (e) {
      _appendLog('SDK初始化失败: $e');
    }
  }

  Future<void> _startRecord() async {
    final keyword = _normalizedKeyword;
    try {
      await _service.startRecordWake(keyword: keyword);
      _appendLog('开始录音唤醒，关键词: $keyword');
      setState(() => _isListening = true);
    } catch (e) {
      _appendLog('开始录音失败: $e');
    }
  }

  Future<void> _stopRecord() async {
    try {
      await _service.stopRecordWake();
      _appendLog('停止录音唤醒');
      setState(() => _isListening = false);
    } catch (e) {
      _appendLog('停止录音失败: $e');
    }
  }

  Future<void> _startFileWake() async {
    final path = _audioPathController.text.trim();
    if (path.isEmpty) {
      _appendLog('请先输入音频文件路径');
      return;
    }
    try {
      await _service.startFileWake(
        keyword: _normalizedKeyword,
        audioPath: path,
      );
      _appendLog('开始上传音频唤醒: $path');
    } catch (e) {
      _appendLog('上传音频唤醒失败: $e');
    }
  }

  String get _normalizedKeyword {
    final text = _keywordController.text.trim();
    return text.isEmpty ? '你好小迪' : text;
  }

  void _handleEvent(OfflineIvwEvent event) {
    if (!mounted) {
      return;
    }
    if (event.type == OfflineIvwEventType.db && event.db != null) {
      setState(() => _db = event.db!);
    } else if (event.type == OfflineIvwEventType.auth) {
      setState(() => _sdkReady = event.raw['code'] == 0);
    } else if (event.type == OfflineIvwEventType.state &&
        event.raw['state'] == 'stopped') {
      setState(() => _isListening = false);
    }
    _appendLog(event.message);
  }

  void _appendLog(String message) {
    final now = DateTime.now();
    final time =
        '${now.hour.toString().padLeft(2, '0')}:${now.minute.toString().padLeft(2, '0')}:${now.second.toString().padLeft(2, '0')}';
    setState(() {
      _logs.add('[$time] $message');
      if (_logs.length > 300) {
        _logs.removeRange(0, _logs.length - 300);
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('离线语音唤醒'),
          bottom: const TabBar(
            tabs: <Widget>[
              Tab(text: '上传音频'),
              Tab(text: '录音'),
            ],
          ),
        ),
        body: Column(
          children: <Widget>[
            _buildHeaderCard(),
            Expanded(
              child: TabBarView(
                children: <Widget>[
                  _buildFileTab(),
                  _buildRecordTab(),
                ],
              ),
            ),
            _buildLogPanel(),
          ],
        ),
      ),
    );
  }

  Widget _buildHeaderCard() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 12, 12, 6),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Text('SDK状态: ${_sdkReady ? "已初始化" : "未初始化"}'),
              const SizedBox(height: 8),
              TextField(
                controller: _keywordController,
                decoration: const InputDecoration(
                  labelText: '唤醒词（支持逗号分隔）',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 8),
              Row(
                children: <Widget>[
                  ElevatedButton(
                    onPressed: _initSdk,
                    child: const Text('初始化SDK'),
                  ),
                  const SizedBox(width: 8),
                  Text('当前分贝: $_db'),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildFileTab() {
    return Padding(
      padding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: <Widget>[
          TextField(
            controller: _audioPathController,
            decoration: const InputDecoration(
              labelText: '音频文件路径（16k/16bit/单声道 PCM）',
              border: OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 12),
          ElevatedButton(
            onPressed: _sdkReady ? _startFileWake : null,
            child: const Text('开始唤醒（上传音频）'),
          ),
          const SizedBox(height: 6),
          const Text(
            '示例: /sdcard/iflytek/ivw/test.pcm',
            style: TextStyle(fontSize: 12, color: Colors.black54),
          ),
        ],
      ),
    );
  }

  Widget _buildRecordTab() {
    return Padding(
      padding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: <Widget>[
          ElevatedButton(
            onPressed: _sdkReady && !_isListening ? _startRecord : null,
            child: const Text('开始录音唤醒'),
          ),
          const SizedBox(height: 8),
          ElevatedButton(
            onPressed: _isListening ? _stopRecord : null,
            child: const Text('停止录音唤醒'),
          ),
        ],
      ),
    );
  }

  Widget _buildLogPanel() {
    return Container(
      height: 220,
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      color: Colors.black87,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              const Expanded(
                child: Text(
                  '运行日志',
                  style: TextStyle(color: Colors.white, fontSize: 13),
                ),
              ),
              TextButton(
                onPressed: () => setState(_logs.clear),
                child: const Text('清空'),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Expanded(
            child: ListView.builder(
              itemCount: _logs.length,
              itemBuilder: (BuildContext context, int index) {
                return Text(
                  _logs[index],
                  style: const TextStyle(color: Colors.white70, fontSize: 12),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
