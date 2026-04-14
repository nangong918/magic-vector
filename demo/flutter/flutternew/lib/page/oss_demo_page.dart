import 'dart:io';

import 'package:cached_network_image/cached_network_image.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:gal/gal.dart';
import 'package:image_picker/image_picker.dart';
import 'package:path_provider/path_provider.dart';

import '../data/remote/oss_remote_api_source.dart';
import '../domain/dto/resp/oss_user_bucket_file_item_row.dart';
import '../domain/model/user_session_model.dart';
import '../manager/user_manager.dart';

/// 对标 Android：TabBarView + 上传页 / 存储桶展开列表（长按操作）。
class OssDemoPage extends StatelessWidget {
  const OssDemoPage({super.key});

  bool _isTourist(UserSessionModel? u) {
    if (u == null) return true;
    return u.userId == 1 ||
        u.accessToken == 'tourist' ||
        u.account == 'tourist' ||
        u.accessToken.isEmpty;
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<UserSessionModel?>(
      future: UserManager.instance.getCurrentUser(),
      builder: (context, snap) {
        final u = snap.data;
        if (_isTourist(u)) {
          return Scaffold(
            appBar: AppBar(title: const Text('OSS Demo')),
            body: Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Text('游客或未登录无法使用 OSS'),
                    const SizedBox(height: 16),
                    TextButton(
                      onPressed: () => Navigator.pop(context),
                      child: const Text('返回'),
                    ),
                  ],
                ),
              ),
            ),
          );
        }
        return DefaultTabController(
          length: 2,
          child: Scaffold(
            appBar: AppBar(
              title: const Text('OSS Demo'),
              bottom: const TabBar(
                tabs: [
                  Tab(text: '上传'),
                  Tab(text: '存储桶'),
                ],
              ),
            ),
            body: TabBarView(
              children: [
                _OssUploadTab(userId: u!.userId.toString()),
                _OssBucketTab(userId: u.userId.toString()),
              ],
            ),
          ),
        );
      },
    );
  }
}

class _OssUploadTab extends StatefulWidget {
  const _OssUploadTab({required this.userId});

  final String userId;

  @override
  State<_OssUploadTab> createState() => _OssUploadTabState();
}

class _OssUploadTabState extends State<_OssUploadTab> {
  final OssRemoteApiSource _api = OssRemoteApiSource();
  final ImagePicker _picker = ImagePicker();
  XFile? _picked;

  Future<void> _pick() async {
    final x = await _picker.pickImage(source: ImageSource.gallery);
    if (!mounted) return;
    setState(() => _picked = x);
  }

  Future<void> _upload() async {
    final f = _picked;
    if (f == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('请先选择图片')),
      );
      return;
    }
    try {
      final path = f.path;
      final name = f.name;
      final res = await _api.batchUploadSingleFile(
        userId: widget.userId,
        bucketName: null,
        filePath: path,
        filename: name,
      );
      final ok = res.items?.any((e) => e.success == true) ?? false;
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(ok ? '上传成功' : (res.items?.firstOrNull?.message ?? '上传失败'))),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('$e')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        children: [
          Expanded(
            child: GestureDetector(
              onTap: _pick,
              child: Container(
                width: double.infinity,
                decoration: BoxDecoration(
                  color: Theme.of(context).colorScheme.surfaceContainerHighest,
                  borderRadius: BorderRadius.circular(12),
                ),
                alignment: Alignment.center,
                child: _picked == null
                    ? const Text('点击选择相册图片')
                    : Image.file(
                        File(_picked!.path),
                        fit: BoxFit.contain,
                      ),
              ),
            ),
          ),
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: _upload,
              child: const Text('上传'),
            ),
          ),
        ],
      ),
    );
  }
}

class _OssBucketTab extends StatefulWidget {
  const _OssBucketTab({required this.userId});

  final String userId;

  @override
  State<_OssBucketTab> createState() => _OssBucketTabState();
}

class _OssBucketTabState extends State<_OssBucketTab> {
  final OssRemoteApiSource _api = OssRemoteApiSource();
  final ImagePicker _picker = ImagePicker();

  List<String> _buckets = [];
  final Map<String, List<OssUserBucketFileItemRow>> _files = {};
  bool _loadingBuckets = true;
  String? _loadingBucket;
  String? _replaceBucket;
  String? _replaceFileId;

  @override
  void initState() {
    super.initState();
    _refreshBuckets();
  }

  Future<void> _refreshBuckets() async {
    setState(() {
      _loadingBuckets = true;
      _files.clear();
    });
    try {
      final res = await _api.userBucketList(userId: widget.userId);
      if (!mounted) return;
      setState(() {
        _buckets = res.bucketNameList ?? [];
        _loadingBuckets = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _loadingBuckets = false);
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    }
  }

  Future<void> _loadFiles(String bucket, {bool force = false}) async {
    if (!force && _files.containsKey(bucket)) return;
    setState(() => _loadingBucket = bucket);
    try {
      final res = await _api.userBucketFileItemList(
        userId: widget.userId,
        bucketName: bucket,
      );
      if (!mounted) return;
      setState(() {
        _files[bucket] = res.items ?? [];
        _loadingBucket = null;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _loadingBucket = null);
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    }
  }

  Future<void> _invalidateBucket(String bucket) async {
    setState(() => _files.remove(bucket));
    await _loadFiles(bucket, force: true);
  }

  Future<void> _download(String url, String rawName) async {
    try {
      final dir = await getTemporaryDirectory();
      final safe = rawName.replaceAll(RegExp(r'[^a-zA-Z0-9._-]'), '_');
      final name = safe.isEmpty ? 'download_${DateTime.now().millisecondsSinceEpoch}.jpg' : safe;
      final path = '${dir.path}/$name';
      await Dio().download(url, path);
      final hasAccess = await Gal.hasAccess();
      if (!hasAccess) {
        await Gal.requestAccess();
      }
      await Gal.putImage(path);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('已保存到相册')),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('下载失败 $e')));
    }
  }

  Future<void> _delete(String bucket, String fileId) async {
    try {
      await _api.batchDeleteFiles([fileId]);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('已删除')));
      await _invalidateBucket(bucket);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    }
  }

  Future<void> _replacePick() async {
    final b = _replaceBucket;
    final fid = _replaceFileId;
    if (b == null || fid == null) return;
    final x = await _picker.pickImage(source: ImageSource.gallery);
    if (x == null) return;
    try {
      final res = await _api.updateFileContent(
        fileId: fid,
        filePath: x.path,
        filename: x.name,
      );
      if (!mounted) return;
      final ok = res.updated == true;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(ok ? '已更换图片' : (res.message ?? '更换失败'))),
      );
      if (ok) await _invalidateBucket(b);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    } finally {
      _replaceBucket = null;
      _replaceFileId = null;
    }
  }

  void _showFileMenu(String bucket, OssUserBucketFileItemRow row) {
    final fid = row.fileId ?? '';
    showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(row.originFileName ?? '文件'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            TextButton(
              onPressed: () {
                Navigator.pop(ctx);
                final u = row.url;
                if (u != null && u.isNotEmpty) {
                  _download(u, row.originFileName ?? 'image.jpg');
                }
              },
              child: const Text('下载到本地'),
            ),
            TextButton(
              onPressed: () {
                Navigator.pop(ctx);
                _replaceBucket = bucket;
                _replaceFileId = fid;
                _replacePick();
              },
              child: const Text('更换图片'),
            ),
            TextButton(
              onPressed: () {
                Navigator.pop(ctx);
                _delete(bucket, fid);
              },
              child: const Text('删除图片'),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('关闭')),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (_loadingBuckets && _buckets.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }
    return RefreshIndicator(
      onRefresh: _refreshBuckets,
      child: ListView.builder(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(12),
        itemCount: _buckets.length,
        itemBuilder: (context, i) {
          final bucket = _buckets[i];
          return Card(
            key: ValueKey(bucket),
            child: ExpansionTile(
              title: Text(bucket),
              onExpansionChanged: (open) {
                if (open) {
                  _loadFiles(bucket);
                }
              },
              children: [
                if (_loadingBucket == bucket)
                  const Padding(
                    padding: EdgeInsets.all(16),
                    child: Center(child: CircularProgressIndicator()),
                  )
                else ...(_files[bucket] ?? []).map(
                  (row) => _FileTile(
                    row: row,
                    onLongPress: () => _showFileMenu(bucket, row),
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _FileTile extends StatelessWidget {
  const _FileTile({
    required this.row,
    required this.onLongPress,
  });

  final OssUserBucketFileItemRow row;
  final VoidCallback onLongPress;

  @override
  Widget build(BuildContext context) {
    final url = row.url;
    return InkWell(
      onLongPress: onLongPress,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
        child: Column(
          children: [
            AspectRatio(
              aspectRatio: 1,
              child: url == null || url.isEmpty
                  ? Container(color: Colors.grey.shade300)
                  : CachedNetworkImage(
                      imageUrl: url,
                      fit: BoxFit.cover,
                      placeholder: (context, url) =>
                          const Center(child: CircularProgressIndicator()),
                      errorWidget: (context, url, error) =>
                          const Icon(Icons.broken_image),
                    ),
            ),
            const SizedBox(height: 6),
            Text(
              row.originFileName ?? row.fileId ?? '',
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ],
        ),
      ),
    );
  }
}
