package com.example.flutteraar.ui.activity;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.flutteraar.R;
import com.example.flutteraar.media.MediaDemoConfig;
import com.example.flutteraar.media.worker.HlsToMp4Worker;
import com.example.flutteraar.media.worker.ResumableUploadWorker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalMediaDemoActivity extends AppCompatActivity {
    private static final String TAG_FILE_HASH_PREFIX = "upload-file-hash-";
    private static final String TAG_HLS_CACHE_HASH_PREFIX = "hls-cache-hash-";
    private static final Pattern FILE_ID_PATTERN = Pattern.compile("^file_(\\d+)$");

    private final List<LocalVideoItem> localVideos = new ArrayList<>();
    private final List<LocalVideoItem> cloudDownloadedVideos = new ArrayList<>();
    private final List<HlsCacheItem> hlsCacheItems = new ArrayList<>();
    private final List<LocalVideoItem> hlsMp4Videos = new ArrayList<>();
    private final Map<String, UploadUiState> uploadStateMap = new HashMap<>();
    private final Map<String, ConvertUiState> hlsConvertStateMap = new HashMap<>();

    private WorkManager workManager;
    private LocalVideoAdapter localAdapter;
    private CloudDownloadedVideoAdapter cloudDownloadedAdapter;
    private HlsCacheAdapter hlsCacheAdapter;
    private HlsMp4Adapter hlsMp4Adapter;
    private TextView tvFolderPath;
    private TextView tvHint;
    private TextView tvCloudDownloadFolderPath;
    private TextView tvCloudDownloadHint;
    private TextView tvHlsCacheFolderPath;
    private TextView tvHlsCacheHint;
    private TextView tvHlsMp4FolderPath;
    private TextView tvHlsMp4Hint;
    private final ActivityResultLauncher<String[]> importVideoLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onVideoImported);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_media_demo);
        setTitle("本地 Media Demo");

        workManager = WorkManager.getInstance(this);
        tvFolderPath = findViewById(R.id.tvLocalVideoFolder);
        tvHint = findViewById(R.id.tvLocalHint);
        tvCloudDownloadFolderPath = findViewById(R.id.tvCloudDownloadFolder);
        tvCloudDownloadHint = findViewById(R.id.tvCloudDownloadHint);
        tvHlsCacheFolderPath = findViewById(R.id.tvHlsCacheFolder);
        tvHlsCacheHint = findViewById(R.id.tvHlsCacheHint);
        tvHlsMp4FolderPath = findViewById(R.id.tvHlsMp4Folder);
        tvHlsMp4Hint = findViewById(R.id.tvHlsMp4Hint);

        Button btnRefresh = findViewById(R.id.btnRefreshLocalVideoList);
        Button btnImport = findViewById(R.id.btnImportOfflineVideo);
        Button btnRefreshCloudDownload = findViewById(R.id.btnRefreshCloudDownloadVideoList);
        Button btnRefreshHlsCache = findViewById(R.id.btnRefreshHlsCacheList);
        Button btnRefreshHlsMp4 = findViewById(R.id.btnRefreshHlsMp4List);

        RecyclerView recyclerLocal = findViewById(R.id.recyclerLocalVideoList);
        recyclerLocal.setLayoutManager(new LinearLayoutManager(this));
        localAdapter = new LocalVideoAdapter();
        recyclerLocal.setAdapter(localAdapter);

        RecyclerView recyclerCloudDownload = findViewById(R.id.recyclerCloudDownloadVideoList);
        recyclerCloudDownload.setLayoutManager(new LinearLayoutManager(this));
        cloudDownloadedAdapter = new CloudDownloadedVideoAdapter();
        recyclerCloudDownload.setAdapter(cloudDownloadedAdapter);

        RecyclerView recyclerHlsCache = findViewById(R.id.recyclerHlsCacheList);
        recyclerHlsCache.setLayoutManager(new LinearLayoutManager(this));
        hlsCacheAdapter = new HlsCacheAdapter();
        recyclerHlsCache.setAdapter(hlsCacheAdapter);

        RecyclerView recyclerHlsMp4 = findViewById(R.id.recyclerHlsMp4List);
        recyclerHlsMp4.setLayoutManager(new LinearLayoutManager(this));
        hlsMp4Adapter = new HlsMp4Adapter();
        recyclerHlsMp4.setAdapter(hlsMp4Adapter);

        btnRefresh.setOnClickListener(v -> scanLocalVideos());
        btnImport.setOnClickListener(v -> importVideoLauncher.launch(new String[]{"video/*"}));
        btnRefreshCloudDownload.setOnClickListener(v -> scanCloudDownloadedVideos());
        btnRefreshHlsCache.setOnClickListener(v -> scanHlsCacheItems());
        btnRefreshHlsMp4.setOnClickListener(v -> scanHlsMp4Videos());

        observeUploadState();
        observeHlsConvertState();
        scanLocalVideos();
        scanCloudDownloadedVideos();
        scanHlsCacheItems();
        scanHlsMp4Videos();
    }

    private void onVideoImported(Uri uri) {
        if (uri == null) {
            return;
        }
        try {
            getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );
        } catch (Exception ignored) {
        }
        new Thread(() -> {
            try {
                String displayName = queryDisplayName(uri);
                if (displayName == null || displayName.trim().isEmpty()) {
                    displayName = "offline_" + System.currentTimeMillis() + ".mp4";
                }
                displayName = sanitizeFileName(displayName);
                File targetDir = MediaDemoConfig.getLocalVideoDir(this);
                File targetFile = buildNonConflictTarget(targetDir, displayName);

                try (InputStream inputStream = getContentResolver().openInputStream(uri);
                     FileOutputStream outputStream = new FileOutputStream(targetFile)) {
                    if (inputStream == null) {
                        throw new IllegalStateException("无法读取所选文件");
                    }
                    byte[] buffer = new byte[32 * 1024];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, len);
                    }
                    outputStream.flush();
                }

                boolean moved = tryDeleteSourceUri(uri);
                String message = moved
                        ? "已移动到离线目录: " + targetFile.getName()
                        : "已复制到离线目录(源文件保留): " + targetFile.getName();
                runOnUiThread(() -> {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    scanLocalVideos();
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "导入失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }, "offline-video-import").start();
    }

    private String queryDisplayName(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (columnIndex >= 0) {
                    return cursor.getString(columnIndex);
                }
            }
            return null;
        } catch (Exception ignored) {
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private boolean tryDeleteSourceUri(Uri uri) {
        try {
            return DocumentsContract.deleteDocument(getContentResolver(), uri);
        } catch (Exception ignored) {
            return false;
        }
    }

    private File buildNonConflictTarget(File dir, String fileName) {
        File target = new File(dir, fileName);
        if (!target.exists()) {
            return target;
        }
        int dotIndex = fileName.lastIndexOf('.');
        String prefix = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        String suffix = dotIndex > 0 ? fileName.substring(dotIndex) : "";
        int index = 1;
        while (target.exists()) {
            target = new File(dir, prefix + "_" + index + suffix);
            index++;
        }
        return target;
    }

    private String sanitizeFileName(String fileName) {
        String safe = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        return safe.trim().isEmpty() ? "offline_" + System.currentTimeMillis() + ".mp4" : safe;
    }

    private void observeUploadState() {
        workManager.getWorkInfosByTagLiveData(ResumableUploadWorker.TAG_UPLOAD).observe(this, workInfos -> {
            Map<String, UploadUiState> latest = new HashMap<>();
            Map<String, String> tagToPath = new HashMap<>();
            for (LocalVideoItem item : localVideos) {
                tagToPath.put(getFileTag(item.file.getAbsolutePath()), item.file.getAbsolutePath());
            }
            for (WorkInfo workInfo : workInfos) {
                String filePath = findPathByTag(workInfo, tagToPath, TAG_FILE_HASH_PREFIX);
                if (TextUtils.isEmpty(filePath)) {
                    continue;
                }
                UploadUiState state = UploadUiState.from(workInfo);
                UploadUiState old = latest.get(filePath);
                if (old == null || state.priority() >= old.priority()) {
                    latest.put(filePath, state);
                }
            }
            uploadStateMap.clear();
            uploadStateMap.putAll(latest);
            localAdapter.notifyDataSetChanged();
        });
    }

    private void observeHlsConvertState() {
        workManager.getWorkInfosByTagLiveData(HlsToMp4Worker.TAG_CONVERT).observe(this, workInfos -> {
            Map<String, ConvertUiState> latest = new HashMap<>();
            Map<String, String> tagToPath = new HashMap<>();
            for (HlsCacheItem item : hlsCacheItems) {
                tagToPath.put(getHlsCacheTag(item.cacheDir.getAbsolutePath()), item.cacheDir.getAbsolutePath());
            }
            boolean hasSuccess = false;
            for (WorkInfo workInfo : workInfos) {
                String cachePath = findPathByTag(workInfo, tagToPath, TAG_HLS_CACHE_HASH_PREFIX);
                if (TextUtils.isEmpty(cachePath)) {
                    continue;
                }
                ConvertUiState state = ConvertUiState.from(workInfo);
                ConvertUiState old = latest.get(cachePath);
                if (old == null || state.priority() >= old.priority()) {
                    latest.put(cachePath, state);
                }
                if (state.isSuccess()) {
                    hasSuccess = true;
                }
            }
            hlsConvertStateMap.clear();
            hlsConvertStateMap.putAll(latest);
            hlsCacheAdapter.notifyDataSetChanged();
            if (hasSuccess) {
                scanHlsMp4Videos();
            }
        });
    }

    private String findPathByTag(WorkInfo workInfo, Map<String, String> tagToPath, String requiredPrefix) {
        for (String tag : workInfo.getTags()) {
            if (tag != null && tag.startsWith(requiredPrefix)) {
                return tagToPath.get(tag);
            }
        }
        return null;
    }

    private void scanLocalVideos() {
        File localVideoDir = MediaDemoConfig.getLocalVideoDir(this);
        tvFolderPath.setText("本地目录: " + localVideoDir.getAbsolutePath());
        scanVideoFiles(localVideoDir, localVideos);
        if (localVideos.isEmpty()) {
            tvHint.setText("当前目录没有视频文件，请先将 mp4/mov/mkv 文件放入上面的固定目录。");
        } else {
            tvHint.setText("视频数量: " + localVideos.size() + "（上传支持断点续传）");
        }
        localAdapter.notifyDataSetChanged();
    }

    private void scanCloudDownloadedVideos() {
        File cloudDownloadDir = MediaDemoConfig.getCloudDownloadDir(this);
        tvCloudDownloadFolderPath.setText("cloud-download目录: " + cloudDownloadDir.getAbsolutePath());
        scanVideoFiles(cloudDownloadDir, cloudDownloadedVideos);
        if (cloudDownloadedVideos.isEmpty()) {
            tvCloudDownloadHint.setText("当前 cloud-download 目录没有可播放视频。");
        } else {
            tvCloudDownloadHint.setText("视频数量: " + cloudDownloadedVideos.size() + "（支持本地预览播放）");
        }
        cloudDownloadedAdapter.notifyDataSetChanged();
    }

    private void scanHlsCacheItems() {
        File hlsCacheDir = MediaDemoConfig.getHlsCacheDir(this);
        tvHlsCacheFolderPath.setText("hls-cache目录: " + hlsCacheDir.getAbsolutePath());
        hlsCacheItems.clear();
        File[] children = hlsCacheDir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (!child.isDirectory()) {
                    continue;
                }
                File playlistFile = findPlaylistFile(child);
                if (playlistFile == null) {
                    continue;
                }
                hlsCacheItems.add(new HlsCacheItem(
                        child,
                        playlistFile,
                        parseFileIdFromFolderName(child.getName()),
                        countTsFiles(child),
                        calculateDirectorySize(child)
                ));
            }
        }
        hlsCacheItems.sort(Comparator.comparingLong((HlsCacheItem item) -> item.cacheDir.lastModified()).reversed());
        if (hlsCacheItems.isEmpty()) {
            tvHlsCacheHint.setText("当前 hls-cache 目录没有可用缓存。");
        } else {
            tvHlsCacheHint.setText("缓存数量: " + hlsCacheItems.size() + "（支持本地 HLS 播放 / FFmpeg 转 MP4）");
        }
        hlsCacheAdapter.notifyDataSetChanged();
    }

    private void scanHlsMp4Videos() {
        File hlsMp4Dir = MediaDemoConfig.getHlsMp4Dir(this);
        tvHlsMp4FolderPath.setText("hls-mp4目录: " + hlsMp4Dir.getAbsolutePath());
        scanVideoFiles(hlsMp4Dir, hlsMp4Videos);
        if (hlsMp4Videos.isEmpty()) {
            tvHlsMp4Hint.setText("当前 hls-mp4 目录没有转码结果。");
        } else {
            tvHlsMp4Hint.setText("视频数量: " + hlsMp4Videos.size() + "（含云播放页和本地缓存页转码结果）");
        }
        hlsMp4Adapter.notifyDataSetChanged();
    }

    private File findPlaylistFile(File cacheDir) {
        File localIndex = new File(cacheDir, "index_local.m3u8");
        if (localIndex.exists() && localIndex.isFile()) {
            return localIndex;
        }
        File[] files = cacheDir.listFiles();
        if (files == null) {
            return null;
        }
        for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".m3u8")) {
                return file;
            }
        }
        return null;
    }

    private String parseFileIdFromFolderName(String folderName) {
        if (folderName == null) {
            return "";
        }
        Matcher matcher = FILE_ID_PATTERN.matcher(folderName);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return "";
    }

    private int countTsFiles(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return 0;
        }
        int count = 0;
        for (File file : files) {
            if (file.isDirectory()) {
                count += countTsFiles(file);
            } else if (file.getName().toLowerCase(Locale.ROOT).endsWith(".ts")) {
                count++;
            }
        }
        return count;
    }

    private long calculateDirectorySize(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return 0L;
        }
        long total = 0L;
        for (File file : files) {
            if (file.isDirectory()) {
                total += calculateDirectorySize(file);
            } else {
                total += file.length();
            }
        }
        return total;
    }

    private void scanVideoFiles(File folder, List<LocalVideoItem> target) {
        target.clear();
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isFile() && isVideoFile(file.getName())) {
                target.add(new LocalVideoItem(file));
            }
        }
        target.sort(Comparator.comparingLong((LocalVideoItem item) -> item.file.lastModified()).reversed());
    }

    private boolean isVideoFile(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".m4v")
                || lower.endsWith(".mkv") || lower.endsWith(".webm");
    }

    private String readableSize(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1fKB", bytes / 1024f);
        }
        if (bytes < 1024L * 1024L * 1024L) {
            return String.format(Locale.getDefault(), "%.1fMB", bytes / 1024f / 1024f);
        }
        return String.format(Locale.getDefault(), "%.1fGB", bytes / 1024f / 1024f / 1024f);
    }

    private void toggleUpload(LocalVideoItem item) {
        String uniqueWorkName = getUniqueWorkName(item.file.getAbsolutePath());
        UploadUiState state = uploadStateMap.get(item.file.getAbsolutePath());
        if (state != null && state.isActive()) {
            workManager.cancelUniqueWork(uniqueWorkName);
            Toast.makeText(this, "已暂停上传: " + item.file.getName(), Toast.LENGTH_SHORT).show();
            return;
        }
        Data data = new Data.Builder()
                .putString(ResumableUploadWorker.KEY_FILE_PATH, item.file.getAbsolutePath())
                .putLong(ResumableUploadWorker.KEY_USER_ID, MediaDemoConfig.DEMO_USER_ID)
                .putString(ResumableUploadWorker.KEY_BUCKET_NAME, MediaDemoConfig.DEMO_BUCKET_NAME)
                .putString(ResumableUploadWorker.KEY_BASE_URL, MediaDemoConfig.SERVER_BASE_URL)
                .build();
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ResumableUploadWorker.class)
                .setConstraints(constraints)
                .setInputData(data)
                .addTag(ResumableUploadWorker.TAG_UPLOAD)
                .addTag(getFileTag(item.file.getAbsolutePath()))
                .build();
        workManager.enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, request);
        Toast.makeText(this, "开始上传: " + item.file.getName(), Toast.LENGTH_SHORT).show();
    }

    private void toggleHlsConvert(HlsCacheItem item) {
        if (TextUtils.isEmpty(item.fileId)) {
            Toast.makeText(this, "无法识别 fileId，不能调用云端 FFmpeg 转码", Toast.LENGTH_LONG).show();
            return;
        }
        String uniqueWorkName = HlsToMp4Worker.buildUniqueWorkName(item.fileId);
        ConvertUiState state = hlsConvertStateMap.get(item.cacheDir.getAbsolutePath());
        if (state != null && state.isActive()) {
            workManager.cancelUniqueWork(uniqueWorkName);
            Toast.makeText(this, "已停止转码: " + item.cacheDir.getName(), Toast.LENGTH_SHORT).show();
            return;
        }
        Data data = new Data.Builder()
                .putString(HlsToMp4Worker.KEY_FILE_ID, item.fileId)
                .putString(HlsToMp4Worker.KEY_FILE_NAME, item.cacheDir.getName())
                .putString(HlsToMp4Worker.KEY_BASE_URL, MediaDemoConfig.SERVER_BASE_URL)
                .build();
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(HlsToMp4Worker.class)
                .setConstraints(constraints)
                .setInputData(data)
                .addTag(HlsToMp4Worker.TAG_CONVERT)
                .addTag(getHlsCacheTag(item.cacheDir.getAbsolutePath()))
                .build();
        workManager.enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, request);
        Toast.makeText(this, "开始 FFmpeg 转码: " + item.cacheDir.getName(), Toast.LENGTH_SHORT).show();
    }

    private String getUniqueWorkName(String filePath) {
        return "video-upload-" + Math.abs(filePath.hashCode());
    }

    private String getFileTag(String filePath) {
        return TAG_FILE_HASH_PREFIX + Math.abs(filePath.hashCode());
    }

    private String getHlsCacheTag(String cachePath) {
        return TAG_HLS_CACHE_HASH_PREFIX + Math.abs(cachePath.hashCode());
    }

    private void playLocal(LocalVideoItem item) {
        Intent intent = new Intent(this, LocalVideoPlayerActivity.class);
        intent.putExtra(LocalVideoPlayerActivity.EXTRA_FILE_PATH, item.file.getAbsolutePath());
        startActivity(intent);
    }

    private void playLocalHls(HlsCacheItem item) {
        Intent intent = new Intent(this, LocalHlsPlayerActivity.class);
        intent.putExtra(LocalHlsPlayerActivity.EXTRA_PLAYLIST_PATH, item.playlistFile.getAbsolutePath());
        startActivity(intent);
    }

    private static class LocalVideoItem {
        private final File file;

        private LocalVideoItem(File file) {
            this.file = file;
        }
    }

    private static class HlsCacheItem {
        private final File cacheDir;
        private final File playlistFile;
        private final String fileId;
        private final int segmentCount;
        private final long totalSize;

        private HlsCacheItem(File cacheDir, File playlistFile, String fileId, int segmentCount, long totalSize) {
            this.cacheDir = cacheDir;
            this.playlistFile = playlistFile;
            this.fileId = fileId;
            this.segmentCount = segmentCount;
            this.totalSize = totalSize;
        }
    }

    private static class UploadUiState {
        private final WorkInfo.State state;
        private final long uploaded;
        private final long total;
        private final String status;

        private UploadUiState(WorkInfo.State state, long uploaded, long total, String status) {
            this.state = state;
            this.uploaded = uploaded;
            this.total = total;
            this.status = status;
        }

        private static UploadUiState from(WorkInfo workInfo) {
            Data progress = workInfo.getProgress();
            Data output = workInfo.getOutputData();
            long uploaded = progress.getLong(ResumableUploadWorker.KEY_UPLOADED,
                    output.getLong(ResumableUploadWorker.KEY_UPLOADED, 0L));
            long total = progress.getLong(ResumableUploadWorker.KEY_TOTAL,
                    output.getLong(ResumableUploadWorker.KEY_TOTAL, 0L));
            String status = progress.getString(ResumableUploadWorker.KEY_STATUS);
            if (status == null) {
                status = output.getString(ResumableUploadWorker.KEY_STATUS);
            }
            return new UploadUiState(workInfo.getState(), uploaded, total, status == null ? "" : status);
        }

        private int priority() {
            if (state == WorkInfo.State.RUNNING) {
                return 5;
            }
            if (state == WorkInfo.State.ENQUEUED) {
                return 4;
            }
            if (state == WorkInfo.State.SUCCEEDED) {
                return 3;
            }
            if (state == WorkInfo.State.FAILED) {
                return 2;
            }
            if (state == WorkInfo.State.CANCELLED) {
                return 1;
            }
            return 0;
        }

        private boolean isActive() {
            return state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED;
        }
    }

    private static class ConvertUiState {
        private final WorkInfo.State state;
        private final long downloaded;
        private final long total;
        private final String status;
        private final String outputPath;

        private ConvertUiState(WorkInfo.State state, long downloaded, long total, String status, String outputPath) {
            this.state = state;
            this.downloaded = downloaded;
            this.total = total;
            this.status = status;
            this.outputPath = outputPath;
        }

        private static ConvertUiState from(WorkInfo workInfo) {
            Data progress = workInfo.getProgress();
            Data output = workInfo.getOutputData();
            long downloaded = progress.getLong(HlsToMp4Worker.KEY_DOWNLOADED,
                    output.getLong(HlsToMp4Worker.KEY_DOWNLOADED, 0L));
            long total = progress.getLong(HlsToMp4Worker.KEY_TOTAL,
                    output.getLong(HlsToMp4Worker.KEY_TOTAL, 0L));
            String status = progress.getString(HlsToMp4Worker.KEY_STATUS);
            if (status == null) {
                status = output.getString(HlsToMp4Worker.KEY_STATUS);
            }
            String outputPath = progress.getString(HlsToMp4Worker.KEY_OUTPUT_PATH);
            if (outputPath == null) {
                outputPath = output.getString(HlsToMp4Worker.KEY_OUTPUT_PATH);
            }
            return new ConvertUiState(workInfo.getState(), downloaded, total,
                    status == null ? "" : status, outputPath == null ? "" : outputPath);
        }

        private int priority() {
            if (state == WorkInfo.State.RUNNING) {
                return 5;
            }
            if (state == WorkInfo.State.ENQUEUED) {
                return 4;
            }
            if (state == WorkInfo.State.SUCCEEDED) {
                return 3;
            }
            if (state == WorkInfo.State.FAILED) {
                return 2;
            }
            if (state == WorkInfo.State.CANCELLED) {
                return 1;
            }
            return 0;
        }

        private boolean isActive() {
            return state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED;
        }

        private boolean isSuccess() {
            return state == WorkInfo.State.SUCCEEDED;
        }
    }

    private class LocalVideoAdapter extends RecyclerView.Adapter<LocalVideoAdapter.LocalVideoViewHolder> {

        @NonNull
        @Override
        public LocalVideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_local_video, parent, false);
            return new LocalVideoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LocalVideoViewHolder holder, int position) {
            LocalVideoItem item = localVideos.get(position);
            holder.progressUpload.setVisibility(View.VISIBLE);
            holder.tvUploadProgress.setVisibility(View.VISIBLE);
            holder.btnUpload.setVisibility(View.VISIBLE);
            holder.btnPlay.setText("播放本地");
            holder.tvFileName.setText(item.file.getName());
            holder.tvFileInfo.setText("大小: " + readableSize(item.file.length()));
            UploadUiState state = uploadStateMap.get(item.file.getAbsolutePath());
            if (state == null) {
                holder.progressUpload.setProgress(0);
                holder.tvUploadProgress.setText("未上传");
                holder.btnUpload.setText("上传云端");
            } else {
                int progress = state.total > 0 ? (int) Math.min(100, (state.uploaded * 100 / state.total)) : 0;
                holder.progressUpload.setProgress(progress);
                String statusText = state.status == null ? "" : state.status;
                if (statusText.isEmpty()) {
                    holder.tvUploadProgress.setText("进度: " + progress + "%  " + state.state.name());
                } else {
                    holder.tvUploadProgress.setText("进度: " + progress + "%  " + state.state.name() + "  " + statusText);
                }
                if (state.isActive()) {
                    holder.btnUpload.setText("暂停上传");
                } else if (state.state == WorkInfo.State.SUCCEEDED) {
                    holder.btnUpload.setText("已上传");
                } else if (state.uploaded > 0) {
                    holder.btnUpload.setText("继续上传");
                } else {
                    holder.btnUpload.setText("上传云端");
                }
            }
            holder.btnPlay.setOnClickListener(v -> playLocal(item));
            holder.btnUpload.setOnClickListener(v -> toggleUpload(item));
        }

        @Override
        public int getItemCount() {
            return localVideos.size();
        }

        class LocalVideoViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvFileName;
            private final TextView tvFileInfo;
            private final TextView tvUploadProgress;
            private final ProgressBar progressUpload;
            private final Button btnPlay;
            private final Button btnUpload;

            LocalVideoViewHolder(@NonNull View itemView) {
                super(itemView);
                tvFileName = itemView.findViewById(R.id.tvLocalFileName);
                tvFileInfo = itemView.findViewById(R.id.tvLocalFileInfo);
                tvUploadProgress = itemView.findViewById(R.id.tvLocalUploadProgress);
                progressUpload = itemView.findViewById(R.id.progressLocalUpload);
                btnPlay = itemView.findViewById(R.id.btnPlayLocal);
                btnUpload = itemView.findViewById(R.id.btnUploadCloud);
            }
        }
    }

    private class CloudDownloadedVideoAdapter extends RecyclerView.Adapter<CloudDownloadedVideoAdapter.CloudDownloadedVideoViewHolder> {

        @NonNull
        @Override
        public CloudDownloadedVideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_local_video, parent, false);
            return new CloudDownloadedVideoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CloudDownloadedVideoViewHolder holder, int position) {
            LocalVideoItem item = cloudDownloadedVideos.get(position);
            holder.tvFileName.setText(item.file.getName());
            holder.tvFileInfo.setText("大小: " + readableSize(item.file.length()));
            holder.progressUpload.setVisibility(View.GONE);
            holder.tvUploadProgress.setVisibility(View.VISIBLE);
            holder.tvUploadProgress.setText("来源: cloud-download");
            holder.btnUpload.setVisibility(View.GONE);
            holder.btnPlay.setText("预览播放");
            holder.btnPlay.setOnClickListener(v -> playLocal(item));
        }

        @Override
        public int getItemCount() {
            return cloudDownloadedVideos.size();
        }

        class CloudDownloadedVideoViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvFileName;
            private final TextView tvFileInfo;
            private final TextView tvUploadProgress;
            private final ProgressBar progressUpload;
            private final Button btnPlay;
            private final Button btnUpload;

            CloudDownloadedVideoViewHolder(@NonNull View itemView) {
                super(itemView);
                tvFileName = itemView.findViewById(R.id.tvLocalFileName);
                tvFileInfo = itemView.findViewById(R.id.tvLocalFileInfo);
                tvUploadProgress = itemView.findViewById(R.id.tvLocalUploadProgress);
                progressUpload = itemView.findViewById(R.id.progressLocalUpload);
                btnPlay = itemView.findViewById(R.id.btnPlayLocal);
                btnUpload = itemView.findViewById(R.id.btnUploadCloud);
            }
        }
    }

    private class HlsCacheAdapter extends RecyclerView.Adapter<HlsCacheAdapter.HlsCacheViewHolder> {

        @NonNull
        @Override
        public HlsCacheViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_local_video, parent, false);
            return new HlsCacheViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull HlsCacheViewHolder holder, int position) {
            HlsCacheItem item = hlsCacheItems.get(position);
            String title = TextUtils.isEmpty(item.fileId) ? item.cacheDir.getName() : item.cacheDir.getName() + " (fileId=" + item.fileId + ")";
            holder.tvFileName.setText(title);
            holder.tvFileInfo.setText("片段: " + item.segmentCount + "  大小: " + readableSize(item.totalSize));
            holder.progressUpload.setVisibility(View.VISIBLE);
            holder.btnUpload.setVisibility(View.VISIBLE);
            holder.btnPlay.setText("播放缓存");
            holder.btnPlay.setOnClickListener(v -> playLocalHls(item));

            ConvertUiState state = hlsConvertStateMap.get(item.cacheDir.getAbsolutePath());
            if (state == null) {
                holder.progressUpload.setProgress(0);
                holder.tvUploadProgress.setText("播放单: " + item.playlistFile.getName());
                if (TextUtils.isEmpty(item.fileId)) {
                    holder.btnUpload.setText("无法转码");
                    holder.btnUpload.setEnabled(false);
                } else {
                    holder.btnUpload.setText("FFmpeg转MP4");
                    holder.btnUpload.setEnabled(true);
                }
            } else {
                int progress = state.total > 0 ? (int) Math.min(100, (state.downloaded * 100 / state.total)) : 0;
                holder.progressUpload.setProgress(progress);
                String status = state.status;
                if (state.isSuccess()) {
                    holder.tvUploadProgress.setText("转码完成: " + state.outputPath);
                    holder.btnUpload.setText("重新转码");
                    holder.btnUpload.setEnabled(!TextUtils.isEmpty(item.fileId));
                } else if (state.isActive()) {
                    if (progress > 0) {
                        holder.tvUploadProgress.setText("转码中: " + progress + "%  " + status);
                    } else {
                        holder.tvUploadProgress.setText("转码中: " + status);
                    }
                    holder.btnUpload.setText("停止转码");
                    holder.btnUpload.setEnabled(true);
                } else {
                    holder.tvUploadProgress.setText("转码状态: " + state.state.name() + "  " + status);
                    holder.btnUpload.setText(TextUtils.isEmpty(item.fileId) ? "无法转码" : "继续转码");
                    holder.btnUpload.setEnabled(!TextUtils.isEmpty(item.fileId));
                }
            }
            holder.btnUpload.setOnClickListener(v -> toggleHlsConvert(item));
        }

        @Override
        public int getItemCount() {
            return hlsCacheItems.size();
        }

        class HlsCacheViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvFileName;
            private final TextView tvFileInfo;
            private final TextView tvUploadProgress;
            private final ProgressBar progressUpload;
            private final Button btnPlay;
            private final Button btnUpload;

            HlsCacheViewHolder(@NonNull View itemView) {
                super(itemView);
                tvFileName = itemView.findViewById(R.id.tvLocalFileName);
                tvFileInfo = itemView.findViewById(R.id.tvLocalFileInfo);
                tvUploadProgress = itemView.findViewById(R.id.tvLocalUploadProgress);
                progressUpload = itemView.findViewById(R.id.progressLocalUpload);
                btnPlay = itemView.findViewById(R.id.btnPlayLocal);
                btnUpload = itemView.findViewById(R.id.btnUploadCloud);
            }
        }
    }

    private class HlsMp4Adapter extends RecyclerView.Adapter<HlsMp4Adapter.HlsMp4ViewHolder> {

        @NonNull
        @Override
        public HlsMp4ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_local_video, parent, false);
            return new HlsMp4ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull HlsMp4ViewHolder holder, int position) {
            LocalVideoItem item = hlsMp4Videos.get(position);
            holder.tvFileName.setText(item.file.getName());
            holder.tvFileInfo.setText("大小: " + readableSize(item.file.length()));
            holder.progressUpload.setVisibility(View.GONE);
            holder.tvUploadProgress.setVisibility(View.VISIBLE);
            holder.tvUploadProgress.setText("来源: hls-mp4");
            holder.btnUpload.setVisibility(View.GONE);
            holder.btnPlay.setText("播放MP4");
            holder.btnPlay.setOnClickListener(v -> playLocal(item));
        }

        @Override
        public int getItemCount() {
            return hlsMp4Videos.size();
        }

        class HlsMp4ViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvFileName;
            private final TextView tvFileInfo;
            private final TextView tvUploadProgress;
            private final ProgressBar progressUpload;
            private final Button btnPlay;
            private final Button btnUpload;

            HlsMp4ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvFileName = itemView.findViewById(R.id.tvLocalFileName);
                tvFileInfo = itemView.findViewById(R.id.tvLocalFileInfo);
                tvUploadProgress = itemView.findViewById(R.id.tvLocalUploadProgress);
                progressUpload = itemView.findViewById(R.id.progressLocalUpload);
                btnPlay = itemView.findViewById(R.id.btnPlayLocal);
                btnUpload = itemView.findViewById(R.id.btnUploadCloud);
            }
        }
    }
}
