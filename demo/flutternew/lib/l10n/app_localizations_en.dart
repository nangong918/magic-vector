// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get appTitle => 'My Awesome App';

  @override
  String get network => 'Network';

  @override
  String get chat => 'Chat';

  @override
  String get native => 'Native';

  @override
  String get xfyun_stt => 'XunFei Sound to Text';

  @override
  String get hello => 'Hello';

  @override
  String get login => 'Login';

  @override
  String get logout => 'Logout';

  @override
  String greetingWithName(String name) {
    return 'Hello $name';
  }
}
