// lib/screens/main_screen.dart
import 'package:flutter/material.dart';
import 'package:flutternew/manager/CatalogManager.dart';

import '../domain/vo/CatalogItem.dart';
import '../ui/CatalogItemList.dart';
import '../l10n/app_localizations.dart';
import 'package:flutter/services.dart';

class MainPage extends StatefulWidget {
  const MainPage({super.key});

  @override
  State<MainPage> createState() => _MainPageState();
}

class _MainPageState extends State<MainPage> {
  String _searchText = '';
  final TextEditingController _searchController = TextEditingController();
  final ScrollController _scrollController = ScrollController();

  Future<String> loadTxtFile() async {
    try {
      // 读取assets中的txt文件，参数是pubspec.yaml中配置的文件路径
      String content = await rootBundle.loadString('assets/txt/clt_agent.txt');
      return content;
    } catch (e) {
      // 捕获异常（比如文件路径错误、文件不存在等）
      return '读取失败：$e';
    }
  }

  // 封装异步逻辑
  void _loadAndPrintTxt() async {
    String txtContent = await loadTxtFile();
    print('txt文本长度：${txtContent.length} \ntxt文件内容：\n$txtContent');
  }

  @override
  void initState() {
    super.initState();
    // 注意：initState 本身不能加 async，所以用匿名异步函数包裹
    _loadAndPrintTxt();
  }

  // 处理列表项点击
  void _onItemClick(CatalogItem item) {
    debugPrint('点击了: ${item.title}');

    // 如果有路由名称，则跳转到对应页面
    if (item.routeName != null && item.routeName!.isNotEmpty) {
      // 调用CatalogManager的统一跳转方法
      CatalogManager.onItemClick(item, context);
    } else {
      // 如果没有设置路由，显示 SnackBar
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('即将打开: ${item.title}'),
          duration: const Duration(milliseconds: 1000),
          action: SnackBarAction(
            label: '确定',
            onPressed: () {},
          ),
        ),
      );
    }
  }


  @override
  void dispose() {
    _searchController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n =
        AppLocalizations.of(context) ?? lookupAppLocalizations(const Locale('zh'));
    final items = CatalogManager.getCatalogItems(l10n);

    return Scaffold(
      // 启用边缘到边缘显示（对应 enableEdgeToEdge）
      extendBodyBehindAppBar: true,
      body: SafeArea(
        child: Column(
          children: [
            // 搜索栏区域
            // _buildSearchBar(theme),

            // 列表内容区域
            Expanded(
              child: items.isEmpty
                  ? _buildEmptyState()
                  : CatalogItemList(
                key: ValueKey(_searchText), // 搜索变化时重建列表
                items: items,
                onItemClick: _onItemClick,
                scrollController: _scrollController,
              ),
            ),
          ],
        ),
      ),
    );
  }

  // 构建空状态
  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.search_off,
            size: 64,
            color: Colors.grey[400],
          ),
          const SizedBox(height: 16),
          Text(
            '没有找到相关内容',
            style: TextStyle(
              fontSize: 18,
              color: Colors.grey[600],
              fontWeight: FontWeight.w500,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            '请尝试其他搜索关键词',
            style: TextStyle(
              fontSize: 14,
              color: Colors.grey[500],
            ),
          ),
          if (_searchText.isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(top: 16),
              child: ElevatedButton.icon(
                onPressed: () {
                  setState(() {
                    _searchText = '';
                    _searchController.clear();
                  });
                },
                icon: const Icon(Icons.refresh),
                label: const Text('清除搜索'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFF48FB1),
                  foregroundColor: Colors.white,
                ),
              ),
            ),
        ],
      ),
    );
  }
}

// ==================== 主题配置 ====================
class AppDemoTheme extends StatelessWidget {
  final Widget child;

  const AppDemoTheme({super.key, required this.child});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      // ========== 新增：国际化核心配置 ==========
      // 初始化国际化
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFFF48FB1), // Pink40
          brightness: Brightness.light,
          primary: const Color(0xFFF48FB1),
          secondary: const Color(0xFFCE93D8),
        ),
        appBarTheme: const AppBarTheme(
          backgroundColor: Color(0xFFF48FB1),
          foregroundColor: Colors.white,
          elevation: 0,
        ),
      ),
      home: child,
    );
  }
}

// ==================== 主应用入口 ====================
class MainActivity extends StatelessWidget {
  const MainActivity({super.key});

  @override
  Widget build(BuildContext context) {
    return const AppDemoTheme(
      child: MainPage(),
    );
  }
}

// ==================== 预览组件 ====================
class MainScreenPreview extends StatelessWidget {
  const MainScreenPreview({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFFF48FB1),
          brightness: Brightness.light,
        ),
      ),
      home: const Scaffold(
        backgroundColor: Colors.white,
        body: MainPage(),
      ),
    );
  }
}

// ==================== 应用入口 ====================
void main() {
  // 运行主应用
  runApp(const MainActivity());

  // 或者运行独立预览（调试用）
  // runApp(const MainScreenPreview());
}