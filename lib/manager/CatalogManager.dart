import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

import '../domain/vo/CatalogItem.dart';



typedef OnClickCatalogItem = void Function(CatalogItem item);

class CatalogManager {
  // 模拟获取目录数据
  static List<CatalogItem> getCatalogItems() {
    return [
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
  }

  // 处理Item点击跳转逻辑
  static void onItemClick(CatalogItem item, BuildContext context) {
    if (item.routeName != null && item.routeName!.isNotEmpty) {
      Navigator.pushNamed(context, item.routeName!);
    }
  }

}