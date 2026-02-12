import 'package:flutter/material.dart';

import 'config/app_route.dart';
import 'l10n/app_localizations.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      onGenerateTitle: (context) =>
          AppLocalizations.of(context)?.appTitle ?? '功能目录',
      debugShowCheckedModeBanner: false,
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      // 核心：使用routes映射表（替代onGenerateRoute）
      routes: appRoutes,
      // 初始路由（对应appRoutes中的AppRoutes.main）
      initialRoute: AppRoutes.main,
      // 可选：兜底处理未知路由（如果需要）
      onUnknownRoute: unknownRoute,
      theme: ThemeData(
        useMaterial3: true,
        primarySwatch: Colors.pink,
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFFF48FB1),
          brightness: Brightness.light,
        ),
        appBarTheme: const AppBarTheme(
          backgroundColor: Color(0xFFF48FB1),
          foregroundColor: Colors.white,
          elevation: 0,
        ),
      ),
    );
  }
}