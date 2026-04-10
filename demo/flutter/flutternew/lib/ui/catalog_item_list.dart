// 懒加载列表组件 - 对应 Compose 的 LazyColumn

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

import '../domain/vo/catalog_item.dart';
import 'select_item_view.dart';

class CatalogItemList extends StatelessWidget {
  final List<CatalogItem> items;
  final void Function(CatalogItem) onItemClick;
  final EdgeInsetsGeometry? padding;
  final ScrollController? scrollController;

  const CatalogItemList({
    super.key,
    required this.items,
    required this.onItemClick,
    this.padding,
    this.scrollController,
  });

  @override
  Widget build(BuildContext context) {
    const pink40 = Color(0xFFF48FB1);

    return ListView.separated(
      controller: scrollController,
      padding: padding ?? EdgeInsets.zero,
      itemCount: items.length,
      itemBuilder: (context, index) {
        final item = items[index];

        return _CatalogListItem(
          key: ValueKey(item.id),
          item: item,
          pink40: pink40,
          onItemClick: () => onItemClick(item),
        );
      },
      separatorBuilder: (context, index) => Divider(
        height: 1,
        thickness: 1.0,
        color: pink40,
        indent: iconIndentForItem(index),
        endIndent: 16.0,
      ),
    );
  }

  double iconIndentForItem(int index) {
    final item = items[index];
    return item.iconRes != null ? 52.0 : 16.0;
  }
}

class _CatalogListItem extends StatelessWidget {
  final CatalogItem item;
  final Color pink40;
  final VoidCallback onItemClick;

  const _CatalogListItem({
    required Key key,
    required this.item,
    required this.pink40,
    required this.onItemClick,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return SelectItemView(
      title: item.title,
      subtitle: item.subtitle,
      onItemClick: onItemClick,
      icon: item.iconRes != null
          ? Icon(
              item.iconRes!,
              color: pink40,
              size: 24.0,
            )
          : null,
    );
  }
}

// ==================== 预览 ====================

final previewItems = [
  const CatalogItem(
    id: "1",
    title: "选项一",
    subtitle: "这是第一个选项的详细描述信息",
    iconRes: CupertinoIcons.chart_bar,
    routeName: "/network",
  ),
  const CatalogItem(
    id: "2",
    title: "选项二",
    subtitle: "第二个选项的副标题内容",
    iconRes: null,
    routeName: "/network",
  ),
  const CatalogItem(
    id: "3",
    title: "选项三",
    subtitle: "这是一个很长的副标题，用来测试文本过长时的显示效果，看看是否会正确换行",
    iconRes: CupertinoIcons.chart_bar,
    routeName: "/network",
  ),
  const CatalogItem(
    id: "4",
    title: "选项四",
    subtitle: null,
    iconRes: CupertinoIcons.chart_bar,
    routeName: "/network",
  ),
  const CatalogItem(
    id: "5",
    title: "选项五",
    subtitle: "最后一个选项",
    iconRes: null,
    routeName: "/network",
  ),
];

class CatalogListWithIconsPreview extends StatelessWidget {
  const CatalogListWithIconsPreview({super.key});

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
      home: Scaffold(
        appBar: AppBar(
          title: const Text('带图标列表'),
          backgroundColor: const Color(0xFFF48FB1),
        ),
        body: CatalogItemList(
          items: previewItems,
          onItemClick: (item) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text('点击了: ${item.title}'),
                duration: const Duration(milliseconds: 800),
              ),
            );
          },
        ),
      ),
    );
  }
}

class CatalogListSimplePreview extends StatelessWidget {
  const CatalogListSimplePreview({super.key});

  final simpleItems = const [
    CatalogItem(id: "1", title: "简洁选项一", subtitle: null, iconRes: null),
    CatalogItem(id: "2", title: "简洁选项二", subtitle: null, iconRes: null),
    CatalogItem(id: "3", title: "简洁选项三", subtitle: null, iconRes: null),
  ];

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      theme: ThemeData(useMaterial3: true),
      home: Scaffold(
        appBar: AppBar(title: const Text('简洁列表')),
        body: CatalogItemList(
          items: simpleItems,
          onItemClick: (item) {},
          padding: const EdgeInsets.all(16.0),
        ),
      ),
    );
  }
}

class CatalogListIconsOnlyPreview extends StatelessWidget {
  const CatalogListIconsOnlyPreview({super.key});

  final iconItems = const [
    CatalogItem(id: "1", title: "图标选项一", subtitle: null, iconRes: CupertinoIcons.chart_bar),
    CatalogItem(id: "2", title: "图标选项二", subtitle: null, iconRes: CupertinoIcons.chart_bar),
    CatalogItem(id: "3", title: "图标选项三", subtitle: null, iconRes: CupertinoIcons.chart_bar),
  ];

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      theme: ThemeData(useMaterial3: true),
      home: Scaffold(
        appBar: AppBar(title: const Text('图标列表')),
        body: CatalogItemList(
          items: iconItems,
          onItemClick: (item) {},
          padding: const EdgeInsets.all(16.0),
        ),
      ),
    );
  }
}

class CatalogListSinglePreview extends StatelessWidget {
  const CatalogListSinglePreview({super.key});

  final singleItem = const [
    CatalogItem(
      id: "1",
      title: "单独选项",
      subtitle: "只有一个项目的列表",
      iconRes: CupertinoIcons.chart_bar,
    ),
  ];

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      theme: ThemeData(useMaterial3: true),
      home: Scaffold(
        appBar: AppBar(title: const Text('单一项')),
        body: CatalogItemList(
          items: singleItem,
          onItemClick: (item) {},
          padding: const EdgeInsets.all(16.0),
        ),
      ),
    );
  }
}

void main() {
  runApp(const CatalogListWithIconsPreview());
}
