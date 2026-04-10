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
      title: 'VectorDemo',
      onGenerateTitle: (context) => 'VectorDemo',
      debugShowCheckedModeBanner: false,
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      // 核心：使用routes映射表（替代onGenerateRoute）
      routes: appRoutes,
      // 初始路由：先走启动页鉴权，再决定去主页面或登录页
      initialRoute: AppRoutes.start,
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