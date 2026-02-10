import 'package:flutter/material.dart';

import '../domain/vo/CatalogItem.dart';

// 自定义Widget（对标Compose自定义View）
class CatalogItemWidget extends StatelessWidget {
  // 接收Item数据和点击回调
  final CatalogItem item;
  final VoidCallback? onTap; // 点击回调（简化版，也可保留原typedef）

  // 必须加const，保证预览性能
  const CatalogItemWidget({
    super.key,
    required this.item,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    // 点击事件封装（对标Compose的clickable）
    return GestureDetector(
      onTap: onTap,
      child: Container(
        // 基础样式（可根据需求调整）
        margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(12),
          boxShadow: [
            BoxShadow(
              color: Colors.grey.withOpacity(0.1),
              blurRadius: 4,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min, // 高度适配内容
          children: [
            // 标题
            Text(
              item.title,
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w600,
                color: Colors.black87,
              ),
            ),
            // 副标题（非空才显示）
            if (item.subtitle != null && item.subtitle!.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Text(
                  item.subtitle!,
                  style: TextStyle(
                    fontSize: 14,
                    color: Colors.grey[600],
                  ),
                ),
              ),
            // 可选：显示routeName（调试用）
            if (item.routeName != null && item.routeName!.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 6),
                child: Text(
                  "路由：${item.routeName}",
                  style: TextStyle(
                    fontSize: 12,
                    color: Colors.blueAccent.withOpacity(0.8),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

// ==================== Flutter 预览实现 ====================
// 方式1：单独的预览Widget（Flutter官方推荐）
@immutable
class CatalogItemWidgetPreview extends StatelessWidget {
  const CatalogItemWidgetPreview({super.key});

  // 模拟你的测试数据
  static const _mockItem = CatalogItem(
    id: "1",
    title: "network",
    subtitle: "network demo",
    iconRes: Icons.network_wifi,
    routeName: "/network",
  );

  @override
  Widget build(BuildContext context) {
    // 预览容器：模拟真实页面环境
    return MaterialApp(
      theme: ThemeData(
        useMaterial3: true,
        primarySwatch: Colors.blue,
      ),
      home: Scaffold(
        backgroundColor: Colors.grey[100],
        body: Center(
          child: CatalogItemWidget(
            item: _mockItem,
            onTap: () {
              // 预览时的点击反馈（可选）
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text("点击了network item")),
              );
            },
          ),
        ),
      ),
    );
  }
}

// 方式2：预览入口函数（可直接运行预览）
void main() {
  runApp(const CatalogItemWidgetPreview());
}