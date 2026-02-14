import 'dart:convert';

class XfIatHeader {
  final int code;
  final String? message;
  final String? sid;
  final int? status;

  XfIatHeader({
    required this.code,
    this.message,
    this.sid,
    this.status,
  });

  factory XfIatHeader.fromJson(Map<String, dynamic> json) {
    return XfIatHeader(
      code: json['code'] ?? -1,
      message: json['message'],
      sid: json['sid'],
      status: json['status'],
    );
  }
}

class XfIatResult {
  final String? text;
  final int? status;

  XfIatResult({
    this.text,
    this.status,
  });

  factory XfIatResult.fromJson(Map<String, dynamic> json) {
    return XfIatResult(
      text: json['text'],
      status: json['status'],
    );
  }
}

class XfIatPayload {
  final XfIatResult? result;

  XfIatPayload({this.result});

  factory XfIatPayload.fromJson(Map<String, dynamic> json) {
    return XfIatPayload(
      result: json['result'] == null ? null : XfIatResult.fromJson(json['result']),
    );
  }
}

class XfIatResponse {
  final XfIatHeader header;
  final XfIatPayload? payload;

  XfIatResponse({
    required this.header,
    this.payload,
  });

  factory XfIatResponse.fromJson(Map<String, dynamic> json) {
    return XfIatResponse(
      header: XfIatHeader.fromJson(json['header'] ?? const {}),
      payload: json['payload'] == null ? null : XfIatPayload.fromJson(json['payload']),
    );
  }
}

class XfIatText {
  final List<XfIatWs> ws;
  final String? pgs;
  final List<int>? rg;

  XfIatText({
    required this.ws,
    this.pgs,
    this.rg,
  });

  factory XfIatText.fromBase64(String base64Text) {
    final decoded = utf8.decode(base64.decode(base64Text));
    final json = jsonDecode(decoded) as Map<String, dynamic>;
    return XfIatText.fromJson(json);
  }

  factory XfIatText.fromJson(Map<String, dynamic> json) {
    return XfIatText(
      ws: (json['ws'] as List<dynamic>? ?? const [])
          .map((e) => XfIatWs.fromJson(e))
          .toList(),
      pgs: json['pgs'],
      rg: (json['rg'] as List<dynamic>?)?.map((e) => e as int).toList(),
    );
  }

  List<String> flattenWords() {
    final words = <String>[];
    for (final item in ws) {
      for (final cw in item.cw) {
        if (cw.w != null) {
          words.add(cw.w!);
        }
      }
    }
    return words;
  }
}

class XfIatWs {
  final List<XfIatCw> cw;

  XfIatWs({required this.cw});

  factory XfIatWs.fromJson(Map<String, dynamic> json) {
    return XfIatWs(
      cw: (json['cw'] as List<dynamic>? ?? const [])
          .map((e) => XfIatCw.fromJson(e))
          .toList(),
    );
  }
}

class XfIatCw {
  final String? w;

  XfIatCw({this.w});

  factory XfIatCw.fromJson(Map<String, dynamic> json) {
    return XfIatCw(w: json['w']);
  }
}
