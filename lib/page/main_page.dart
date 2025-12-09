// lib/screens/main_screen.dart
import 'package:flutter/material.dart';
import 'package:flutter3_app/manager/CatalogManager.dart';

import '../domain/vo/CatalogItem.dart';
import '../ui/CatalogItemList.dart';

class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  String _searchText = '';
  final TextEditingController _searchController = TextEditingController();
  final ScrollController _scrollController = ScrollController();



  // 处理列表项点击
  void _onItemClick(CatalogItem item) {
    // 在这里处理点击事件，比如导航到对应页面
    print('点击了: ${item.title}');

    // 显示SnackBar反馈
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

    // 实际项目中这里应该进行页面跳转
    // if (item.routeName != null) {
    //   Navigator.pushNamed(context, item.routeName!);
    // }
  }

  @override
  void dispose() {
    _searchController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {

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
              child: CatalogManager.getCatalogItems().isEmpty
                  ? _buildEmptyState()
                  : CatalogItemList(
                key: ValueKey(_searchText), // 搜索变化时重建列表
                items: CatalogManager.getCatalogItems(),
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
      child: MainScreen(),
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
        body: MainScreen(),
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