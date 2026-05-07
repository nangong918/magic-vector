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

public class LocalMediaDemoActivity extends AppCompatActivity {
    private static final String TAG_FILE_HASH_PREFIX = "upload-file-hash-";

    private final List<LocalVideoItem> localVideos = new ArrayList<>();
    private final Map<String, UploadUiState> uploadStateMap = new HashMap<>();

    private WorkManager workManager;
    private LocalVideoAdapter adapter;
    private TextView tvFolderPath;
    private TextView tvHint;
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
        Button btnRefresh = findViewById(R.id.btnRefreshLocalVideoList);
        Button btnImport = findViewById(R.id.btnImportOfflineVideo);
        RecyclerView recyclerView = findViewById(R.id.recyclerLocalVideoList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LocalVideoAdapter();
        recyclerView.setAdapter(adapter);

        btnRefresh.setOnClickListener(v -> scanLocalVideos());
        btnImport.setOnClickListener(v -> importVideoLauncher.launch(new String[]{"video/*"}));
        observeUploadState();
        scanLocalVideos();
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
                String filePath = findFilePathByTags(workInfo, tagToPath);
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
            adapter.notifyDataSetChanged();
        });
    }

    private void scanLocalVideos() {
        File localVideoDir = MediaDemoConfig.getLocalVideoDir(this);
        tvFolderPath.setText("本地目录: " + localVideoDir.getAbsolutePath());
        localVideos.clear();
        File[] files = localVideoDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && isVideoFile(file.getName())) {
                    localVideos.add(new LocalVideoItem(file));
                }
            }
            localVideos.sort(Comparator.comparingLong((LocalVideoItem item) -> item.file.lastModified()).reversed());
        }
        if (localVideos.isEmpty()) {
            tvHint.setText("当前目录没有视频文件，请先将 mp4/mov/mkv 文件放入上面的固定目录。");
        } else {
            tvHint.setText("视频数量: " + localVideos.size() + "（上传支持断点续传）");
        }
        adapter.notifyDataSetChanged();
    }

    private boolean isVideoFile(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".m4v") || lower.endsWith(".mkv") || lower.endsWith(".webm");
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

    private String getUniqueWorkName(String filePath) {
        return "video-upload-" + Math.abs(filePath.hashCode());
    }

    private String getFileTag(String filePath) {
        return TAG_FILE_HASH_PREFIX + Math.abs(filePath.hashCode());
    }

    private String findFilePathByTags(WorkInfo workInfo, Map<String, String> tagToPath) {
        for (String tag : workInfo.getTags()) {
            if (tag != null && tag.startsWith(TAG_FILE_HASH_PREFIX)) {
                return tagToPath.get(tag);
            }
        }
        return null;
    }

    private void playLocal(LocalVideoItem item) {
        Intent intent = new Intent(this, LocalVideoPlayerActivity.class);
        intent.putExtra(LocalVideoPlayerActivity.EXTRA_FILE_PATH, item.file.getAbsolutePath());
        startActivity(intent);
    }

    private static class LocalVideoItem {
        private final File file;

        private LocalVideoItem(File file) {
            this.file = file;
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
    }
}
