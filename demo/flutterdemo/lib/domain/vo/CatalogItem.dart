// lib/domain/vo/catalog_item.dart

// lib/models/catalog_item.dart
import 'package:flutter/cupertino.dart';

class CatalogItem {
  final String id; // 唯一key（对标Compose的it.id）
  final String title;
  final String? subtitle;
  final IconData? iconRes; // 图标资源标识（对标Compose的iconRes）
  final String? routeName; // 跳转路由（替代Compose的cls）

  const CatalogItem({
    required this.id,
    required this.title,
    this.subtitle,
    this.iconRes,
    this.routeName,
  });
}