import 'dart:convert';

import 'package:flutter/services.dart';

class ModuleKeyConfigException implements Exception {
  final String message;

  const ModuleKeyConfigException(this.message);

  @override
  String toString() => message;
}

class XfLlmKeyConfig {
  final String hostUrl;
  final String appId;
  final String apiKey;
  final String apiSecret;
  final String domain;
  final String patchId;
  final String serviceId;
  final String resourceId;

  const XfLlmKeyConfig({
    required this.hostUrl,
    required this.appId,
    required this.apiKey,
    required this.apiSecret,
    required this.domain,
    required this.patchId,
    required this.serviceId,
    required this.resourceId,
  });

  factory XfLlmKeyConfig.fromJson(Map<String, dynamic> json) {
    return XfLlmKeyConfig(
      hostUrl: (json['hostUrl'] ?? '').toString(),
      appId: (json['appId'] ?? '').toString(),
      apiKey: (json['apiKey'] ?? '').toString(),
      apiSecret: (json['apiSecret'] ?? '').toString(),
      domain: (json['domain'] ?? '').toString(),
      patchId: (json['patchId'] ?? '').toString(),
      serviceId: (json['serviceId'] ?? '').toString(),
      resourceId: (json['resourceId'] ?? '').toString(),
    );
  }

  void validate() {
    if (hostUrl.isEmpty || appId.isEmpty || apiKey.isEmpty || apiSecret.isEmpty) {
      throw const ModuleKeyConfigException(
        'module_key.json 缺少 LLM 配置: hostUrl/appId/apiKey/apiSecret',
      );
    }
    if (domain.isEmpty) {
      throw const ModuleKeyConfigException('module_key.json 缺少 LLM 配置: domain');
    }
  }
}

class XfSttKeyConfig {
  final String hostUrl;
  final String appId;
  final String apiKey;
  final String apiSecret;
  final String serviceId;
  final String resourceId;

  const XfSttKeyConfig({
    required this.hostUrl,
    required this.appId,
    required this.apiKey,
    required this.apiSecret,
    required this.serviceId,
    required this.resourceId,
  });

  factory XfSttKeyConfig.fromJson(Map<String, dynamic> json) {
    return XfSttKeyConfig(
      hostUrl: (json['hostUrl'] ?? '').toString(),
      appId: (json['appId'] ?? '').toString(),
      apiKey: (json['apiKey'] ?? '').toString(),
      apiSecret: (json['apiSecret'] ?? '').toString(),
      serviceId: (json['serviceId'] ?? '').toString(),
      resourceId: (json['resourceId'] ?? '').toString(),
    );
  }

  void validate() {
    if (hostUrl.isEmpty || appId.isEmpty || apiKey.isEmpty || apiSecret.isEmpty) {
      throw const ModuleKeyConfigException(
        'module_key.json 缺少 STT 配置: hostUrl/appId/apiKey/apiSecret',
      );
    }
  }
}

class AliSttKeyConfig {
  final String hostUrl;
  final String apiKey;
  final String model;
  final String format;
  final int sampleRate;
  final bool disfluencyRemovalEnabled;
  final List<String> languageHints;

  const AliSttKeyConfig({
    required this.hostUrl,
    required this.apiKey,
    required this.model,
    required this.format,
    required this.sampleRate,
    required this.disfluencyRemovalEnabled,
    required this.languageHints,
  });

  factory AliSttKeyConfig.fromJson(Map<String, dynamic> json) {
    return AliSttKeyConfig(
      hostUrl: (json['hostUrl'] ?? 'wss://dashscope.aliyuncs.com/api-ws/v1/inference/').toString(),
      apiKey: (json['apiKey'] ?? '').toString(),
      model: (json['model'] ?? 'paraformer-realtime-v2').toString(),
      format: (json['format'] ?? 'pcm').toString(),
      sampleRate: _toInt(json['sampleRate']) ?? 16000,
      disfluencyRemovalEnabled: json['disfluencyRemovalEnabled'] == true,
      languageHints: _toStringList(json['languageHints']),
    );
  }

  static int? _toInt(dynamic value) {
    if (value is int) {
      return value;
    }
    return int.tryParse(value?.toString() ?? '');
  }

  static List<String> _toStringList(dynamic value) {
    if (value is! List) {
      return const <String>[];
    }
    return value.map((dynamic item) => item.toString()).toList(growable: false);
  }

  void validate() {
    if (apiKey.isEmpty) {
      throw const ModuleKeyConfigException('module_key.json 缺少 STT_ALI 配置: apiKey');
    }
    if (hostUrl.isEmpty) {
      throw const ModuleKeyConfigException('module_key.json 缺少 STT_ALI 配置: hostUrl');
    }
    if (sampleRate <= 0) {
      throw const ModuleKeyConfigException('module_key.json STT_ALI 配置错误: sampleRate 必须大于 0');
    }
    if (model.isEmpty || format.isEmpty) {
      throw const ModuleKeyConfigException(
        'module_key.json 缺少 STT_ALI 配置: model/format',
      );
    }
  }
}

class XfOfflineIvwKeyConfig {
  final String appId;
  final String apiKey;
  final String apiSecret;
  final String abilityId;
  final String serviceId;
  final String resourceId;

  const XfOfflineIvwKeyConfig({
    required this.appId,
    required this.apiKey,
    required this.apiSecret,
    required this.abilityId,
    required this.serviceId,
    required this.resourceId,
  });

  factory XfOfflineIvwKeyConfig.fromJson(Map<String, dynamic> json) {
    return XfOfflineIvwKeyConfig(
      appId: (json['appId'] ?? '').toString(),
      apiKey: (json['apiKey'] ?? '').toString(),
      apiSecret: (json['apiSecret'] ?? '').toString(),
      abilityId: (json['abilityId'] ?? '').toString(),
      serviceId: (json['serviceId'] ?? '').toString(),
      resourceId: (json['resourceId'] ?? '').toString(),
    );
  }

  void validate() {
    if (appId.isEmpty || apiKey.isEmpty || apiSecret.isEmpty) {
      throw const ModuleKeyConfigException(
        'module_key.json 缺少离线唤醒配置: appId/apiKey/apiSecret',
      );
    }
  }

  Map<String, String> toChannelArgs() {
    return <String, String>{
      'appId': appId,
      'apiKey': apiKey,
      'apiSecret': apiSecret,
      'abilityId': abilityId,
      'serviceId': serviceId,
      'resourceId': resourceId,
    };
  }
}

class ModuleKeyConfig {
  final XfLlmKeyConfig llm;
  final XfSttKeyConfig stt;
  final AliSttKeyConfig sttAli;
  final XfOfflineIvwKeyConfig offlineIvw;

  const ModuleKeyConfig({
    required this.llm,
    required this.stt,
    required this.sttAli,
    required this.offlineIvw,
  });

  factory ModuleKeyConfig.fromJson(Map<String, dynamic> json) {
    final xfyun = (json['xfyun'] as Map?)?.cast<String, dynamic>() ?? <String, dynamic>{};
    final sttAliJson = (json['stt_ali'] as Map?)?.cast<String, dynamic>() ?? <String, dynamic>{};
    return ModuleKeyConfig(
      llm: XfLlmKeyConfig.fromJson(
        (xfyun['llm'] as Map?)?.cast<String, dynamic>() ?? <String, dynamic>{},
      ),
      stt: XfSttKeyConfig.fromJson(
        (xfyun['stt'] as Map?)?.cast<String, dynamic>() ?? <String, dynamic>{},
      ),
      sttAli: AliSttKeyConfig.fromJson(sttAliJson),
      offlineIvw: XfOfflineIvwKeyConfig.fromJson(
        (xfyun['offlineIvw'] as Map?)?.cast<String, dynamic>() ?? <String, dynamic>{},
      ),
    );
  }

  void validate() {
    llm.validate();
    stt.validate();
    sttAli.validate();
    offlineIvw.validate();
  }
}

class ModuleKeyConfigStore {
  ModuleKeyConfigStore._();

  static Future<ModuleKeyConfig>? _future;

  static Future<ModuleKeyConfig> load() {
    return _future ??= _loadInternal();
  }

  static Future<ModuleKeyConfig> _loadInternal() async {
    final content = await rootBundle.loadString('assets/module_key.json');
    final decoded = jsonDecode(content);
    if (decoded is! Map<String, dynamic>) {
      throw const ModuleKeyConfigException('module_key.json 根节点必须是对象');
    }
    final config = ModuleKeyConfig.fromJson(decoded);
    config.validate();
    return config;
  }
}
