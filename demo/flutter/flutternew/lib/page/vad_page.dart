import 'dart:async';

import 'package:flutter/material.dart';

import '../service/vad_service.dart';

class VadPage extends StatefulWidget {
  const VadPage({super.key});

  @override
  State<VadPage> createState() => _VadPageState();
}

class _VadPageState extends State<VadPage> {
  final VadService _service = VadService();
  final List<String> _logs = <String>[];
  static const double _logPanelHeight = 300;

  StreamSubscription<VadEvent>? _eventSub;
  VadEngine _selectedEngine = VadEngine.webrtc;

  List<String> _sampleRates = <String>[];
  List<String> _frameSizes = <String>[];
  List<String> _modes = <String>[];
  String? _selectedSampleRate;
  String? _selectedFrameSize;
  String? _selectedMode;

  bool _isRunning = false;
  int _db = 0;

  @override
  void initState() {
    super.initState();
    _eventSub = _service.events.listen(_onEvent);
    _initPage();
  }

  @override
  void dispose() {
    _eventSub?.cancel();
    _service.release();
    _service.dispose();
    super.dispose();
  }

  Future<void> _initPage() async {
    await _service.init();
    await _reloadOptions(resetSelection: true);
  }

  Future<void> _reloadOptions({bool resetSelection = false}) async {
    final options = await _service.getOptions(engine: _selectedEngine);
    String? nextSampleRate = _selectedSampleRate;
    String? nextMode = _selectedMode;
    if (resetSelection || !options.sampleRates.contains(nextSampleRate)) {
      nextSampleRate = _defaultSampleRate(_selectedEngine, options.sampleRates);
    }
    if (resetSelection || !options.modes.contains(nextMode)) {
      nextMode = _defaultMode(_selectedEngine, options.modes);
    }

    final frameOptions = await _service.getOptions(
      engine: _selectedEngine,
      sampleRate: nextSampleRate,
    );
    String? nextFrameSize = _selectedFrameSize;
    if (resetSelection || !frameOptions.frameSizes.contains(nextFrameSize)) {
      nextFrameSize = _defaultFrameSize(
        _selectedEngine,
        frameOptions.frameSizes,
      );
    }

    if (!mounted) {
      return;
    }
    setState(() {
      _sampleRates = options.sampleRates;
      _modes = options.modes;
      _frameSizes = frameOptions.frameSizes;
      _selectedSampleRate = nextSampleRate;
      _selectedFrameSize = nextFrameSize;
      _selectedMode = nextMode;
    });
  }

  Future<void> _onEngineChanged(VadEngine engine) async {
    if (_selectedEngine == engine) {
      return;
    }
    await _service.stopVad(engine: _selectedEngine);
    if (!mounted) {
      return;
    }
    setState(() {
      _selectedEngine = engine;
      _isRunning = false;
    });
    await _reloadOptions(resetSelection: true);
  }

  Future<void> _onSampleRateChanged(String? value) async {
    if (value == null) {
      return;
    }
    setState(() => _selectedSampleRate = value);
    final options = await _service.getOptions(
      engine: _selectedEngine,
      sampleRate: value,
    );
    if (!mounted) {
      return;
    }
    setState(() {
      _frameSizes = options.frameSizes;
      if (!_frameSizes.contains(_selectedFrameSize)) {
        _selectedFrameSize = _frameSizes.isNotEmpty ? _frameSizes.first : null;
      }
    });
  }

  Future<void> _startVad() async {
    final sampleRate = _selectedSampleRate;
    final frameSize = _selectedFrameSize;
    final mode = _selectedMode;
    if (sampleRate == null || frameSize == null || mode == null) {
      _appendLog('参数未就绪，请先选择采样率/帧长/模式');
      return;
    }

    try {
      final granted = await _service.requestRecordPermission();
      if (!granted) {
        _appendLog('录音权限未授予，无法开始VAD');
        return;
      }
      await _service.startVad(
        engine: _selectedEngine,
        sampleRate: sampleRate,
        frameSize: frameSize,
        mode: mode,
      );
      setState(() => _isRunning = true);
      _appendLog('开始VAD: ${_engineLabel(_selectedEngine)}');
    } catch (e) {
      _appendLog('启动失败: $e');
    }
  }

  Future<void> _stopVad() async {
    try {
      await _service.stopVad(engine: _selectedEngine);
      setState(() => _isRunning = false);
      _appendLog('已停止VAD');
    } catch (e) {
      _appendLog('停止失败: $e');
    }
  }

  void _onEvent(VadEvent event) {
    if (!mounted) {
      return;
    }
    if (event.engine == _selectedEngine) {
      if (event.type == VadEventType.db && event.db != null) {
        setState(() => _db = event.db!);
      } else if (event.type == VadEventType.state && event.running != null) {
        setState(() => _isRunning = event.running!);
      }
    }
    final engineTag = _engineLabel(event.engine);
    _appendLog('[$engineTag] ${event.message}');
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
    return Scaffold(
      appBar: AppBar(
        title: const Text('VAD测试'),
      ),
      body: Column(
        children: <Widget>[
          Expanded(
            child: SingleChildScrollView(
              child: _buildConfigCard(),
            ),
          ),
          SizedBox(
            height: _logPanelHeight,
            child: _buildLogPanel(),
          ),
        ],
      ),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _selectedEngine.index,
        onTap: (int index) => _onEngineChanged(VadEngine.values[index]),
        items: const <BottomNavigationBarItem>[
          BottomNavigationBarItem(
            icon: Icon(Icons.hearing),
            label: 'WebRTC',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.graphic_eq),
            label: 'Silero',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.multitrack_audio),
            label: 'Yamnet',
          ),
        ],
      ),
    );
  }

  Widget _buildConfigCard() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 12, 12, 6),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: <Widget>[
              Text('当前引擎: ${_engineLabel(_selectedEngine)}'),
              const SizedBox(height: 6),
              Text('状态: ${_isRunning ? "运行中" : "未运行"}，当前分贝: $_db'),
              const SizedBox(height: 10),
              _buildDropdown(
                label: '采样率',
                value: _selectedSampleRate,
                items: _sampleRates,
                onChanged: _onSampleRateChanged,
              ),
              const SizedBox(height: 8),
              _buildDropdown(
                label: '帧长',
                value: _selectedFrameSize,
                items: _frameSizes,
                onChanged: (String? v) => setState(() => _selectedFrameSize = v),
              ),
              const SizedBox(height: 8),
              _buildDropdown(
                label: '模式',
                value: _selectedMode,
                items: _modes,
                onChanged: (String? v) => setState(() => _selectedMode = v),
              ),
              const SizedBox(height: 10),
              Row(
                children: <Widget>[
                  Expanded(
                    child: ElevatedButton(
                      onPressed: !_isRunning ? _startVad : null,
                      child: const Text('开始'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: ElevatedButton(
                      onPressed: _isRunning ? _stopVad : null,
                      child: const Text('停止'),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildDropdown({
    required String label,
    required String? value,
    required List<String> items,
    required ValueChanged<String?> onChanged,
  }) {
    return InputDecorator(
      decoration: InputDecoration(
        labelText: label,
        border: const OutlineInputBorder(),
        isDense: true,
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<String>(
          value: items.contains(value) ? value : null,
          isExpanded: true,
          hint: const Text('请选择'),
          items: items
              .map(
                (String e) => DropdownMenuItem<String>(
                  value: e,
                  child: Text(e),
                ),
              )
              .toList(),
          onChanged: onChanged,
        ),
      ),
    );
  }

  Widget _buildLogPanel() {
    return Container(
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
                  style: const TextStyle(color: Colors.white70, fontSize: 14),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  String _engineLabel(VadEngine engine) {
    switch (engine) {
      case VadEngine.webrtc:
        return 'WebRTC';
      case VadEngine.silero:
        return 'Silero';
      case VadEngine.yamnet:
        return 'Yamnet';
    }
  }

  String? _defaultSampleRate(VadEngine engine, List<String> values) {
    if (values.isEmpty) {
      return null;
    }
    switch (engine) {
      case VadEngine.webrtc:
      case VadEngine.silero:
        return _pick(values, 'SAMPLE_RATE_8K');
      case VadEngine.yamnet:
        return values.first;
    }
  }

  String? _defaultFrameSize(VadEngine engine, List<String> values) {
    if (values.isEmpty) {
      return null;
    }
    switch (engine) {
      case VadEngine.webrtc:
        return _pick(values, 'FRAME_SIZE_240');
      case VadEngine.silero:
        return _pick(values, 'FRAME_SIZE_256');
      case VadEngine.yamnet:
        return values.first;
    }
  }

  String? _defaultMode(VadEngine engine, List<String> values) {
    if (values.isEmpty) {
      return null;
    }
    switch (engine) {
      case VadEngine.webrtc:
        return _pick(values, 'VERY_AGGRESSIVE');
      case VadEngine.silero:
        return _pick(values, 'NORMAL');
      case VadEngine.yamnet:
        return values.first;
    }
  }

  String _pick(List<String> values, String preferred) {
    return values.contains(preferred) ? preferred : values.first;
  }
}
