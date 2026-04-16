import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:gal/gal.dart';
import 'package:path_provider/path_provider.dart';

import '../domain/model/oss/oss_bucket_file_item_model.dart';
import '../domain/model/user_session_model.dart';
import '../manager/oss_manager.dart';
import '../manager/user_manager.dart';

sealed class OssDemoIntent {
  const OssDemoIntent();
}

class OssInitialize extends OssDemoIntent {
  const OssInitialize();
}

class OssRefreshBuckets extends OssDemoIntent {
  const OssRefreshBuckets();
}

class OssToggleBucket extends OssDemoIntent {
  final String bucket;
  const OssToggleBucket(this.bucket);
}

class OssRequestMainImagePick extends OssDemoIntent {
  const OssRequestMainImagePick();
}

class OssMainImagePicked extends OssDemoIntent {
  final String? path;
  final String? name;
  const OssMainImagePicked({this.path, this.name});
}

class OssUploadSubmit extends OssDemoIntent {
  const OssUploadSubmit();
}

class OssDownloadImage extends OssDemoIntent {
  final String url;
  final String displayName;
  const OssDownloadImage(this.url, this.displayName);
}

class OssRequestReplacePick extends OssDemoIntent {
  final String bucket;
  final String fileId;
  const OssRequestReplacePick(this.bucket, this.fileId);
}

class OssReplaceImagePicked extends OssDemoIntent {
  final String? path;
  final String? name;
  const OssReplaceImagePicked({this.path, this.name});
}

class OssDeleteFile extends OssDemoIntent {
  final String bucket;
  final String fileId;
  const OssDeleteFile(this.bucket, this.fileId);
}

class OssDemoReplacePending {
  final String bucket;
  final String fileId;
  const OssDemoReplacePending(this.bucket, this.fileId);
}

class OssDemoState {
  final bool touristBlocked;
  final String userIdWire;
  final List<String> buckets;
  final Set<String> expandedBuckets;
  final Map<String, List<OssBucketFileItemModel>> filesByBucket;
  final bool loadingBuckets;
  final String? loadingBucket;
  final String? pickedImagePath;
  final String? pickedImageName;
  final OssDemoReplacePending? replacePending;

  const OssDemoState({
    required this.touristBlocked,
    required this.userIdWire,
    required this.buckets,
    required this.expandedBuckets,
    required this.filesByBucket,
    required this.loadingBuckets,
    required this.loadingBucket,
    required this.pickedImagePath,
    required this.pickedImageName,
    required this.replacePending,
  });

  factory OssDemoState.initial() => const OssDemoState(
        touristBlocked: true,
        userIdWire: '',
        buckets: [],
        expandedBuckets: {},
        filesByBucket: {},
        loadingBuckets: false,
        loadingBucket: null,
        pickedImagePath: null,
        pickedImageName: null,
        replacePending: null,
      );

  OssDemoState copyWith({
    bool? touristBlocked,
    String? userIdWire,
    List<String>? buckets,
    Set<String>? expandedBuckets,
    Map<String, List<OssBucketFileItemModel>>? filesByBucket,
    bool? loadingBuckets,
    String? loadingBucket,
    String? pickedImagePath,
    String? pickedImageName,
    OssDemoReplacePending? replacePending,
    bool clearPickedImage = false,
    bool clearReplacePending = false,
    bool clearLoadingBucket = false,
  }) {
    return OssDemoState(
      touristBlocked: touristBlocked ?? this.touristBlocked,
      userIdWire: userIdWire ?? this.userIdWire,
      buckets: buckets ?? this.buckets,
      expandedBuckets: expandedBuckets ?? this.expandedBuckets,
      filesByBucket: filesByBucket ?? this.filesByBucket,
      loadingBuckets: loadingBuckets ?? this.loadingBuckets,
      loadingBucket:
          clearLoadingBucket ? null : (loadingBucket ?? this.loadingBucket),
      pickedImagePath:
          clearPickedImage ? null : (pickedImagePath ?? this.pickedImagePath),
      pickedImageName:
          clearPickedImage ? null : (pickedImageName ?? this.pickedImageName),
      replacePending: clearReplacePending
          ? null
          : (replacePending ?? this.replacePending),
    );
  }
}

sealed class OssDemoEffect {
  const OssDemoEffect();
}

class OssShowSnack extends OssDemoEffect {
  final String message;
  const OssShowSnack(this.message);
}

class OssOpenMainImagePicker extends OssDemoEffect {
  const OssOpenMainImagePicker();
}

class OssOpenReplaceImagePicker extends OssDemoEffect {
  const OssOpenReplaceImagePicker();
}

class OssDemoVm extends ChangeNotifier {
  OssDemoVm({OssManager? ossManager})
      : _oss = ossManager ?? OssManager.instance {
    unawaited(processIntent(const OssInitialize()));
  }

  final OssManager _oss;
  final _effect = StreamController<OssDemoEffect>.broadcast();

  OssDemoState _state = OssDemoState.initial();
  OssDemoState get state => _state;

  Stream<OssDemoEffect> get effects => _effect.stream;

  bool _isTourist(UserSessionModel? u) {
    if (u == null) return true;
    return u.userId <= 0 ||
        u.userId == 1 ||
        u.accessToken.isEmpty ||
        u.accessToken == 'tourist' ||
        u.account == 'tourist';
  }

  Future<void> processIntent(OssDemoIntent intent) async {
    if (intent is OssInitialize) {
      await _initialize();
    } else if (intent is OssRefreshBuckets) {
      await _refreshBuckets();
    } else if (intent is OssToggleBucket) {
      _toggleBucket(intent.bucket);
    } else if (intent is OssRequestMainImagePick) {
      _effect.add(const OssOpenMainImagePicker());
    } else if (intent is OssMainImagePicked) {
      final p = intent.path;
      if (p == null || p.isEmpty) {
        _state = _state.copyWith(clearPickedImage: true);
      } else {
        _state = _state.copyWith(
          pickedImagePath: p,
          pickedImageName: intent.name,
        );
      }
      notifyListeners();
    } else if (intent is OssUploadSubmit) {
      await _upload();
    } else if (intent is OssDownloadImage) {
      await _download(intent.url, intent.displayName);
    } else if (intent is OssRequestReplacePick) {
      _state = _state.copyWith(
        replacePending: OssDemoReplacePending(intent.bucket, intent.fileId),
      );
      notifyListeners();
      _effect.add(const OssOpenReplaceImagePicker());
    } else if (intent is OssReplaceImagePicked) {
      await _replaceImagePicked(intent.path, intent.name);
    } else if (intent is OssDeleteFile) {
      await _delete(intent.bucket, intent.fileId);
    }
  }

  Future<void> _initialize() async {
    final u = await UserManager.instance.getCurrentUser();
    final blocked = _isTourist(u);
    final wire = (!blocked && u != null) ? u.userId.toString() : '';
    _state = _state.copyWith(
      touristBlocked: blocked,
      userIdWire: wire,
    );
    notifyListeners();
    if (!blocked && wire.isNotEmpty) {
      await _refreshBuckets();
    }
  }

  Future<void> _refreshBuckets() async {
    if (_state.touristBlocked) return;
    final uid = _state.userIdWire;
    final expandedBefore = Set<String>.from(_state.expandedBuckets);
    _state = _state.copyWith(
      loadingBuckets: true,
      filesByBucket: {},
      clearLoadingBucket: true,
    );
    notifyListeners();
    try {
      final model = await _oss.syncUserBucketList(uid);
      final names = model.bucketNames;
      final stillExpanded =
          expandedBefore.intersection(names.toSet()).toSet();
      _state = _state.copyWith(
        loadingBuckets: false,
        buckets: names,
        expandedBuckets: stillExpanded,
      );
      notifyListeners();
      for (final b in stillExpanded) {
        await _loadBucketFiles(b, force: true);
      }
    } catch (e) {
      _state = _state.copyWith(loadingBuckets: false);
      notifyListeners();
      _effect.add(OssShowSnack('$e'));
    }
  }

  void _toggleBucket(String bucket) {
    final next = Set<String>.from(_state.expandedBuckets);
    if (next.contains(bucket)) {
      next.remove(bucket);
    } else {
      next.add(bucket);
    }
    _state = _state.copyWith(expandedBuckets: next);
    notifyListeners();
    if (next.contains(bucket)) {
      unawaited(_loadBucketFiles(bucket, force: false));
    }
  }

  Future<void> _loadBucketFiles(String bucket, {required bool force}) async {
    if (_state.touristBlocked) return;
    if (!force && _state.filesByBucket.containsKey(bucket)) return;
    final uid = _state.userIdWire;
    _state = _state.copyWith(loadingBucket: bucket);
    notifyListeners();
    try {
      final list = await _oss.syncBucketFileItemList(
        userId: uid,
        bucketName: bucket,
      );
      final map = Map<String, List<OssBucketFileItemModel>>.from(
        _state.filesByBucket,
      );
      map[bucket] = list;
      _state = _state.copyWith(
        filesByBucket: map,
        clearLoadingBucket: true,
      );
      notifyListeners();
    } catch (e) {
      _state = _state.copyWith(clearLoadingBucket: true);
      notifyListeners();
      _effect.add(OssShowSnack('$e'));
    }
  }

  Future<void> _invalidateBucket(String bucket) async {
    final map = Map<String, List<OssBucketFileItemModel>>.from(
      _state.filesByBucket,
    );
    map.remove(bucket);
    _state = _state.copyWith(filesByBucket: map);
    notifyListeners();
    if (_state.expandedBuckets.contains(bucket)) {
      await _loadBucketFiles(bucket, force: true);
    }
  }

  Future<void> _upload() async {
    if (_state.touristBlocked) return;
    final path = _state.pickedImagePath;
    final name = _state.pickedImageName;
    if (path == null || path.isEmpty) {
      _effect.add(const OssShowSnack('请先选择图片'));
      return;
    }
    try {
      final res = await _oss.batchUploadSingle(
        userId: _state.userIdWire,
        bucketName: null,
        filePath: path,
        filename: name ?? 'upload.jpg',
      );
      final ok = res.items?.any((e) => e.success == true) ?? false;
      final firstMsg = (res.items != null && res.items!.isNotEmpty)
          ? res.items!.first.message
          : null;
      _effect.add(
        OssShowSnack(ok ? '上传成功' : (firstMsg ?? '上传失败')),
      );
    } catch (e) {
      _effect.add(OssShowSnack('$e'));
    }
  }

  Future<void> _download(String url, String rawName) async {
    try {
      final dir = await getTemporaryDirectory();
      final safe = rawName.replaceAll(RegExp(r'[^a-zA-Z0-9._-]'), '_');
      final name = safe.isEmpty
          ? 'download_${DateTime.now().millisecondsSinceEpoch}.jpg'
          : safe;
      final path = '${dir.path}/$name';
      await Dio().download(url, path);
      final hasAccess = await Gal.hasAccess();
      if (!hasAccess) {
        await Gal.requestAccess();
      }
      await Gal.putImage(path);
      _effect.add(OssShowSnack('下载成功\n$path'));
    } catch (e) {
      _effect.add(OssShowSnack('下载失败 $e'));
    }
  }

  Future<void> _replaceImagePicked(String? path, String? name) async {
    final pending = _state.replacePending;
    _state = _state.copyWith(clearReplacePending: true);
    notifyListeners();
    if (pending == null || path == null || path.isEmpty) {
      return;
    }
    try {
      final res = await _oss.updateFileContent(
        fileId: pending.fileId,
        filePath: path,
        filename: name ?? 'upload.jpg',
      );
      final ok = res.updated == true;
      _effect.add(
        OssShowSnack(ok ? '已更换图片' : (res.message ?? '更换失败')),
      );
      if (ok) await _invalidateBucket(pending.bucket);
    } catch (e) {
      _effect.add(OssShowSnack('$e'));
    }
  }

  Future<void> _delete(String bucket, String fileId) async {
    try {
      await _oss.batchDeleteFiles([fileId]);
      _effect.add(const OssShowSnack('已删除'));
      await _invalidateBucket(bucket);
    } catch (e) {
      _effect.add(OssShowSnack('$e'));
    }
  }

  @override
  void dispose() {
    _effect.close();
    super.dispose();
  }
}
