import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

import '../domain/vo/catalog_item.dart';

class CatalogItemWidget extends StatelessWidget {
  final CatalogItem item;
  final VoidCallback? onTap;

  const CatalogItemWidget({
    super.key,
    required this.item,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
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
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              item.title,
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w600,
                color: Colors.black87,
              ),
            ),
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

@immutable
class CatalogItemWidgetPreview extends StatelessWidget {
  const CatalogItemWidgetPreview({super.key});

  static const _mockItem = CatalogItem(
    id: "1",
    title: "network",
    subtitle: "network demo",
    iconRes: Icons.network_wifi,
    routeName: "/network",
  );

  @override
  Widget build(BuildContext context) {
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

void main() {
  runApp(const CatalogItemWidgetPreview());
}
