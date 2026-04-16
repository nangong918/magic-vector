import 'dart:io';

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';

import '../../domain/model/oss/oss_bucket_file_item_model.dart';
import '../../viewmodel/oss_demo_vm.dart';

typedef OssDispatch = Future<void> Function(OssDemoIntent intent);

class OssDemoScreen extends StatelessWidget {
  const OssDemoScreen({
    super.key,
    required this.state,
    required this.dispatch,
    required this.onBack,
  });

  final OssDemoState state;
  final OssDispatch dispatch;
  final VoidCallback onBack;

  @override
  Widget build(BuildContext context) {
    if (state.touristBlocked) {
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
                TextButton(onPressed: onBack, child: const Text('返回')),
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
            _UploadTab(state: state, dispatch: dispatch),
            _BucketTab(state: state, dispatch: dispatch),
          ],
        ),
      ),
    );
  }
}

class _UploadTab extends StatelessWidget {
  const _UploadTab({required this.state, required this.dispatch});

  final OssDemoState state;
  final OssDispatch dispatch;

  @override
  Widget build(BuildContext context) {
    final path = state.pickedImagePath;
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        children: [
          Expanded(
            child: GestureDetector(
              onTap: () => dispatch(const OssRequestMainImagePick()),
              child: Container(
                width: double.infinity,
                decoration: BoxDecoration(
                  color: Theme.of(context).colorScheme.surfaceContainerHighest,
                  borderRadius: BorderRadius.circular(12),
                ),
                alignment: Alignment.center,
                child: path == null || path.isEmpty
                    ? const Text('点击选择相册图片')
                    : Image.file(
                        File(path),
                        fit: BoxFit.contain,
                      ),
              ),
            ),
          ),
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: () => dispatch(const OssUploadSubmit()),
              child: const Text('上传'),
            ),
          ),
        ],
      ),
    );
  }
}

class _BucketTab extends StatelessWidget {
  const _BucketTab({required this.state, required this.dispatch});

  final OssDemoState state;
  final OssDispatch dispatch;

  @override
  Widget build(BuildContext context) {
    if (state.loadingBuckets && state.buckets.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }
    return RefreshIndicator(
      onRefresh: () => dispatch(const OssRefreshBuckets()),
      child: ListView.builder(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(12),
        itemCount: state.buckets.length,
        itemBuilder: (context, i) {
          final bucket = state.buckets[i];
          final expanded = state.expandedBuckets.contains(bucket);
          final loading = state.loadingBucket == bucket;
          final files = state.filesByBucket[bucket] ?? const [];
          return Card(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                ListTile(
                  title: Text(expanded ? '▼ $bucket' : '▶ $bucket'),
                  trailing: loading
                      ? const SizedBox(
                          width: 22,
                          height: 22,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : null,
                  onTap: () => dispatch(OssToggleBucket(bucket)),
                ),
                if (expanded) ...[
                  if (files.isEmpty && !loading)
                    const Padding(
                      padding: EdgeInsets.fromLTRB(16, 0, 16, 16),
                      child: Text('暂无文件', style: TextStyle(fontSize: 13)),
                    )
                  else
                    ...files.map(
                      (row) => _FileTile(
                        row: row,
                        bucket: bucket,
                        dispatch: dispatch,
                      ),
                    ),
                ],
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
    required this.bucket,
    required this.dispatch,
  });

  final OssBucketFileItemModel row;
  final String bucket;
  final OssDispatch dispatch;

  void _menu(BuildContext context) {
    showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(row.originFileName.isEmpty ? '文件' : row.originFileName),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            TextButton(
              onPressed: () {
                Navigator.pop(ctx);
                final u = row.url;
                if (u.isNotEmpty) {
                  dispatch(
                    OssDownloadImage(
                      u,
                      row.originFileName.isEmpty
                          ? 'image.jpg'
                          : row.originFileName,
                    ),
                  );
                }
              },
              child: const Text('下载到本地'),
            ),
            TextButton(
              onPressed: () {
                Navigator.pop(ctx);
                dispatch(
                  OssRequestReplacePick(bucket, row.fileIdWire),
                );
              },
              child: const Text('更换图片'),
            ),
            TextButton(
              onPressed: () {
                Navigator.pop(ctx);
                dispatch(OssDeleteFile(bucket, row.fileIdWire));
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
    final url = row.url;
    return InkWell(
      onLongPress: () => _menu(context),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
        child: Column(
          children: [
            AspectRatio(
              aspectRatio: 1,
              child: url.isEmpty
                  ? Container(color: Colors.grey.shade300)
                  : CachedNetworkImage(
                      imageUrl: url,
                      fit: BoxFit.cover,
                      placeholder: (c, u) =>
                          const Center(child: CircularProgressIndicator()),
                      errorWidget: (c, u, e) => const Icon(Icons.broken_image),
                    ),
            ),
            const SizedBox(height: 6),
            Text(
              row.originFileName.isEmpty ? row.fileIdWire : row.originFileName,
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
