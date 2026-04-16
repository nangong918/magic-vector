import 'dart:async';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import '../ui/oss/oss_demo_screen.dart';
import '../viewmodel/oss_demo_vm.dart';

/// OSS Demo：Page 处理 Effect（SnackBar、相册）；Screen 只收 state + dispatch（MVI）。
class OssDemoPage extends StatefulWidget {
  const OssDemoPage({super.key});

  @override
  State<OssDemoPage> createState() => _OssDemoPageState();
}

class _OssDemoPageState extends State<OssDemoPage> {
  late final OssDemoVm _vm;
  StreamSubscription<OssDemoEffect>? _effectSub;
  final ImagePicker _picker = ImagePicker();

  @override
  void initState() {
    super.initState();
    _vm = OssDemoVm();
    _effectSub = _vm.effects.listen(_onEffect);
  }

  Future<void> _onEffect(OssDemoEffect effect) async {
    if (!mounted) return;
    if (effect is OssShowSnack) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(effect.message)),
      );
    } else if (effect is OssOpenMainImagePicker) {
      final x = await _picker.pickImage(source: ImageSource.gallery);
      if (!mounted) return;
      await _vm.processIntent(
        OssMainImagePicked(path: x?.path, name: x?.name),
      );
    } else if (effect is OssOpenReplaceImagePicker) {
      final x = await _picker.pickImage(source: ImageSource.gallery);
      if (!mounted) return;
      await _vm.processIntent(
        OssReplaceImagePicked(path: x?.path, name: x?.name),
      );
    }
  }

  @override
  void dispose() {
    _effectSub?.cancel();
    _vm.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _vm,
      builder: (_, _) {
        return OssDemoScreen(
          state: _vm.state,
          dispatch: _vm.processIntent,
          onBack: () => Navigator.pop(context),
        );
      },
    );
  }
}
