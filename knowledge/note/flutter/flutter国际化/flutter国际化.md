**flutter国际化**
====



### Flutter国际化

```yaml
dependencies:
  # 国际化
  flutter_localizations:
    sdk: flutter

  # 国际化
  intl: ^0.20.2

# Flutter框架配置
flutter:
  uses-material-design: true # 启用Material Design图标/组件
  generate: true # 开启代码生成
```

在lib目录下创建l10n
写入app_en.arb
和app_zh.arb
格式类似json
然后执行
```shell
flutter gen-l10n
```
然后就会生成`app_localizations_xx.dart`

然后在 MaterialApp 中启用国际化

```dart
import '../l10n/app_localizations.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

class AppDemoTheme extends StatelessWidget {
  final Widget child;

  const AppDemoTheme({super.key, required this.child});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      // ========== 新增：国际化核心配置 ==========
      localizationsDelegates: const [
        AppLocalizations.delegate,
        GlobalMaterialLocalizations.delegate, // 材质组件国际化
        GlobalWidgetsLocalizations.delegate, // Widgets 国际化
        GlobalCupertinoLocalizations.delegate, // 苹果风格组件国际化
      ],
      supportedLocales: const [
        Locale('en'), // 支持英文
        Locale('zh'), // 支持中文（对应你的 AppLocalizationsZh）
      ],
      home: child,
    );
  }
}
```


使用国际化
```dart
final l10n = AppLocalizations.of(context);
final String title = l10n.network;
```






