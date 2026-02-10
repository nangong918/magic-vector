import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter3_app/config/app_route.dart';

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
        routeName: AppRoutes.network,
      ),
      const CatalogItem(
        id: "2",
        title: "chat",
        subtitle: "chatList demo",
        iconRes: CupertinoIcons.chat_bubble,
        routeName: AppRoutes.chat,
      ),
      const CatalogItem(
        id: "3",
        title: "animation",
        subtitle: "animation demo",
        iconRes: Icons.android_sharp,
        routeName: AppRoutes.native,
      ),
      const CatalogItem(
        id: "4",
        title: "xfyun_stt",
        subtitle: "科大讯飞语音识别",
        iconRes: Icons.mic,
        routeName: AppRoutes.xfyunStt,
      ),
    ];
  }

  // 处理Item点击跳转逻辑
  static void onItemClick(CatalogItem item, BuildContext context) {
    if (item.routeName != null && item.routeName!.isNotEmpty) {
      print('将要跳转到${item.routeName}');
      Navigator.pushNamed(context, item.routeName!);
    } else {
      // 如果没有设置路由，显示提示
      _showFeatureComingSoon(context, item.title);
    }
  }


  // 显示功能开发中提示
  static void _showFeatureComingSoon(BuildContext context, String title) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('$title 功能正在开发中'),
        duration: const Duration(seconds: 2),
        action: SnackBarAction(
          label: '知道了',
          onPressed: () {},
        ),
      ),
    );
  }

}