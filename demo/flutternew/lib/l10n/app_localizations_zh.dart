// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Chinese (`zh`).
class AppLocalizationsZh extends AppLocalizations {
  AppLocalizationsZh([String locale = 'zh']) : super(locale);

  @override
  String get appTitle => '我的牛逼应用';

  @override
  String get network => '网络';

  @override
  String get chat => '聊天';

  @override
  String get native => '原生';

  @override
  String get xfyun_stt => '讯飞语音识别';

  @override
  String get hello => '你好';

  @override
  String get login => '登录';

  @override
  String get logout => '退出登录';

  @override
  String greetingWithName(String name) {
    return '你好 $name';
  }
}
