import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

import '../domain/vo/CatalogItem.dart';

// 自定义Item Widget（对标Compose的SelectItemView）
class SelectItemView extends StatelessWidget {
  final String title;
  final String? subtitle;
  final VoidCallback onItemClick;
  final Widget? icon; // 可选图标

  const SelectItemView({
    super.key,
    required this.title,
    this.subtitle,
    required this.onItemClick,
    this.icon,
  });

  @override
  Widget build(BuildContext context) {
    // 点击容器
    return GestureDetector(
      onTap: onItemClick,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            // 左侧图标（可选）
            if (icon != null)
              Padding(
                padding: const EdgeInsets.only(right: 12),
                child: icon,
              ),
            // 右侧文本区域（占满剩余宽度）
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  // 标题
                  Text(
                    title,
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w500,
                      color: Colors.black87,
                    ),
                  ),
                  // 副标题（非空才显示）
                  if (subtitle != null && subtitle!.isNotEmpty)
                    Padding(
                      padding: const EdgeInsets.only(top: 4),
                      child: Text(
                        subtitle!,
                        style: TextStyle(
                          fontSize: 14,
                          color: Colors.grey[600],
                          // 文本过长自动换行
                          overflow: TextOverflow.visible,
                        ),
                        maxLines: null, // 取消行数限制（对标Compose自动换行）
                      ),
                    ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// 列表Widget（对标Compose的CatalogItemList）
class CatalogItemList extends StatelessWidget {
  final List<CatalogItem> items;
  final Function(CatalogItem) onItemClick;
  final EdgeInsetsGeometry? padding;

  const CatalogItemList({
    super.key,
    required this.items,
    required this.onItemClick,
    this.padding, // 接收 padding 参数
  });

  @override
  Widget build(BuildContext context) {
    // 粉色主题色（对标Compose的Pink40）
    const pink40 = Color(0xFFF48FB1);

    // ListView.builder 对标 Compose LazyColumn（懒加载）
    return ListView.builder(
      padding: EdgeInsets.zero,
      itemCount: items.length,
      // 唯一key（对标Compose的key = { it.id }）
      itemBuilder: (context, index) {
        final item = items[index];
        final IconData? iconRes = item.iconRes;
        return Column(
          children: [

            // 自定义Item
            SelectItemView(
              title: item.title,
              subtitle: item.subtitle,
              onItemClick: () => onItemClick(item),
              icon: item.iconRes != null ?
              Icon(
                item.iconRes!,  // 直接使用 IconData
                color: pink40,
                size: 24,
              ) : null,
            ),
            // 分割线（对标Compose的HorizontalDivider）
            const Divider(
              thickness: 1,
              color: pink40,
              indent: 16, // 左侧缩进（对齐Item内边距）
              endIndent: 16,
            ),
          ],
        );
      },
    );
  }
}

// ==================== 所有预览实现（对标Compose的多个@Preview） ====================
// 预览1：带图标列表（对标SelectItemListWithIconsPreview）
class CatalogItemListWithIconsPreview extends StatelessWidget {
  const CatalogItemListWithIconsPreview({super.key});

  // 预览数据（对标Compose的previewItems）
  static final previewItems = [
    const CatalogItem(
      id: "1",
      title: "network",
      subtitle: "network demo",
      iconRes: Icons.network_wifi,
      routeName: "/network",
    ),
    const CatalogItem(
      id: "2",
      title: "chat",
      subtitle: "chatList demo",
      iconRes: CupertinoIcons.chat_bubble,
      routeName: "/chat",
    ),
  ];

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      theme: ThemeData(useMaterial3: true),
      home: Scaffold(
        backgroundColor: Colors.white,
        body: Padding(
          padding: const EdgeInsets.all(16),
          child: CatalogItemList(
            items: previewItems,
            onItemClick: (item) {
              // 预览点击反馈
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text("点击了：${item.title}")),
              );
            },
          ),
        ),
      ),
    );
  }
}

// 预览2：简洁列表（对标SelectItemListSimplePreview）
class CatalogItemListSimplePreview extends StatelessWidget {
  const CatalogItemListSimplePreview({super.key});

  static final simpleItems = [
    const CatalogItem(id: "1", title: "简洁选项一", subtitle: null, iconRes: null),
    const CatalogItem(id: "2", title: "简洁选项二", subtitle: null, iconRes: null),
    const CatalogItem(id: "3", title: "简洁选项三", subtitle: null, iconRes: null),
  ];

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      theme: ThemeData(useMaterial3: true),
      home: Scaffold(
        backgroundColor: Colors.white,
        body: Padding(
          padding: const EdgeInsets.all(16),
          child: CatalogItemList(
            items: simpleItems,
            onItemClick: (item) {},
          ),
        ),
      ),
    );
  }
}

// 预览3：图标列表（对标SelectItemListIconsOnlyPreview）
class CatalogItemListIconsOnlyPreview extends StatelessWidget {
  const CatalogItemListIconsOnlyPreview({super.key});

  static final iconItems = [
    const CatalogItem(id: "1", title: "图标选项一", subtitle: null, iconRes: CupertinoIcons.chart_bar),
    const CatalogItem(id: "2", title: "图标选项二", subtitle: null, iconRes: CupertinoIcons.calendar),
    const CatalogItem(id: "3", title: "图标选项三", subtitle: null, iconRes: CupertinoIcons.calendar_circle_fill),
  ];

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      theme: ThemeData(useMaterial3: true),
      home: Scaffold(
        backgroundColor: Colors.white,
        body: Padding(
          padding: const EdgeInsets.all(16),
          child: CatalogItemList(
            items: iconItems,
            onItemClick: (item) {},
          ),
        ),
      ),
    );
  }
}

// 预览4：单一项（对标SelectItemListSinglePreview）
class CatalogItemListSinglePreview extends StatelessWidget {
  const CatalogItemListSinglePreview({super.key});

  static final singleItem = [
    const CatalogItem(
      id: "1",
      title: "单独选项",
      subtitle: "只有一个项目的列表",
      iconRes: CupertinoIcons.airplane,
    ),
  ];

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      theme: ThemeData(useMaterial3: true),
      home: Scaffold(
        backgroundColor: Colors.white,
        body: Padding(
          padding: const EdgeInsets.all(16),
          child: CatalogItemList(
            items: singleItem,
            onItemClick: (item) {},
          ),
        ),
      ),
    );
  }
}

// 预览入口：可单独运行某个预览
void main() {
  // 运行「带图标列表」预览（可切换为其他预览）
  runApp(const CatalogItemListWithIconsPreview());
  // 运行「简洁列表」预览：runApp(const CatalogItemListSimplePreview());
  // 运行「图标列表」预览：runApp(const CatalogItemListIconsOnlyPreview());
  // 运行「单一项」预览：runApp(const CatalogItemListSinglePreview());
}