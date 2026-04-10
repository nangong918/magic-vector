import 'package:flutter/cupertino.dart';

class CatalogItem {
  final String id;
  final String title;
  final String? subtitle;
  final IconData? iconRes;
  final String? routeName;

  const CatalogItem({
    required this.id,
    required this.title,
    this.subtitle,
    this.iconRes,
    this.routeName,
  });
}
