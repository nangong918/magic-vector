import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';
import 'package:flutter/material.dart';
import 'package:flutternew/config/app_route.dart';
import 'package:flutternew/service/live_android_service.dart';

import '../domain/vo/catalog_item.dart';
import '../l10n/app_localizations.dart';

typedef OnClickCatalogItem = void Function(CatalogItem item);

class CatalogManager {
  static List<CatalogItem> getCatalogItems(AppLocalizations l10n) {
    return [
      CatalogItem(
        id: "1",
        title: l10n.network,
        subtitle: "network demo",
        iconRes: Icons.network_wifi,
        routeName: AppRoutes.network,
      ),
      CatalogItem(
        id: "2",
        title: l10n.chat,
        subtitle: "chatList demo",
        iconRes: CupertinoIcons.chat_bubble,
        routeName: AppRoutes.chat,
      ),
      CatalogItem(
        id: "3",
        title: l10n.native,
        subtitle: "native demo",
        iconRes: Icons.android_sharp,
        routeName: AppRoutes.native,
      ),
      CatalogItem(
        id: "4",
        title: l10n.xfyun_stt,
        subtitle: "科大讯飞语音识别",
        iconRes: Icons.mic,
        routeName: AppRoutes.xfyunStt,
      ),
      CatalogItem(
        id: "5",
        title: "离线语音唤醒",
        subtitle: "AIKit IVW（上传音频/录音）",
        iconRes: Icons.record_voice_over,
        routeName: AppRoutes.offlineIvw,
      ),
      CatalogItem(
        id: "6",
        title: "VAD测试",
        subtitle: "WebRTC/Silero/Yamnet",
        iconRes: Icons.hearing,
        routeName: AppRoutes.vad,
      ),
      CatalogItem(
        id: "7",
        title: "Voice Agent",
        subtitle: "唤醒 + VAD + STT + Agent",
        iconRes: Icons.smart_toy,
        routeName: AppRoutes.voiceAgent,
      ),
      CatalogItem(
        id: "8",
        title: "OSS Demo",
        subtitle: "上传、存储桶与图片管理",
        iconRes: Icons.cloud_upload,
        routeName: AppRoutes.ossDemo,
      ),
      CatalogItem(
        id: "9",
        title: "Live Push Demo (Android)",
        subtitle: "MethodChannel 调起 Android 原生推流",
        iconRes: Icons.videocam,
        routeName: AppRoutes.livePushAndroid,
      ),
      CatalogItem(
        id: "10",
        title: "Live Pull Demo (Android)",
        subtitle: "MethodChannel 调起 Android 原生拉流",
        iconRes: Icons.live_tv,
        routeName: AppRoutes.livePullAndroid,
      ),
      CatalogItem(
        id: "11",
        title: "Live Push Demo (跨平台)",
        subtitle: "Dart + rtmp_broadcaster（Android/iOS）",
        iconRes: Icons.camera_front,
        routeName: AppRoutes.livePushCrossPlatform,
      ),
      CatalogItem(
        id: "12",
        title: "Live Pull Demo (跨平台)",
        subtitle: "Dart + flutter_vlc_player（Android/iOS）",
        iconRes: Icons.play_circle_fill,
        routeName: AppRoutes.livePullCrossPlatform,
      ),
    ];
  }

  static void onItemClick(CatalogItem item, BuildContext context) {
    if (item.routeName != null && item.routeName!.isNotEmpty) {
      if (item.routeName == AppRoutes.livePushAndroid) {
        _openAndroidLiveDemo(context, isPush: true);
        return;
      }
      if (item.routeName == AppRoutes.livePullAndroid) {
        _openAndroidLiveDemo(context, isPush: false);
        return;
      }
      debugPrint('将要跳转到${item.routeName}');
      Navigator.pushNamed(context, item.routeName!);
    } else {
      _showFeatureComingSoon(context, item.title);
    }
  }

  static Future<void> _openAndroidLiveDemo(
    BuildContext context, {
    required bool isPush,
  }) async {
    try {
      if (isPush) {
        await LiveAndroidService.openLivePushDemo();
      } else {
        await LiveAndroidService.openLivePullDemo();
      }
    } on PlatformException catch (e) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('打开原生页面失败: ${e.message ?? e.code}'),
          duration: const Duration(seconds: 2),
        ),
      );
    }
  }

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
