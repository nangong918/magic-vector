package com.example.flutteraar.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.flutteraar.R;
import com.example.flutteraar.media.MediaDemoConfig;
import com.example.flutteraar.media.SimpleHttpClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CloudMediaDemoActivity extends AppCompatActivity {
    private final ExecutorService ioExecutor = Executors.newCachedThreadPool();
    private final List<CloudVideoItem> cloudVideos = new ArrayList<>();

    private CloudVideoAdapter adapter;
    private TextView tvHint;
    private TextView tvDownloadFolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloud_media_demo);
        setTitle("云上 Media Demo");
        bindViews();
        loadCloudVideos();
    }

    private void bindViews() {
        tvHint = findViewById(R.id.tvCloudHint);
        tvDownloadFolder = findViewById(R.id.tvCloudDownloadFolder);
        tvDownloadFolder.setText("下载目录: " + MediaDemoConfig.getCloudDownloadDir(this).getAbsolutePath());
        Button btnRefresh = findViewById(R.id.btnRefreshCloudList);
        RecyclerView recyclerView = findViewById(R.id.recyclerCloudVideoList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CloudVideoAdapter();
        recyclerView.setAdapter(adapter);
        btnRefresh.setOnClickListener(v -> loadCloudVideos());
    }

    private void loadCloudVideos() {
        tvHint.setText("正在加载云上视频...");
        ioExecutor.execute(() -> {
            try {
                String url = MediaDemoConfig.SERVER_BASE_URL
                        + "/video/cloud/list?userId=" + MediaDemoConfig.DEMO_USER_ID
                        + "&bucketName=" + MediaDemoConfig.DEMO_BUCKET_NAME;
                JSONObject response = SimpleHttpClient.getJson(url);
                JSONObject data = requireResponseData(response);
                JSONArray items = data.optJSONArray("items");
                List<CloudVideoItem> result = new ArrayList<>();
                if (items != null) {
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.optJSONObject(i);
                        if (item == null) {
                            continue;
                        }
                        CloudVideoItem row = new CloudVideoItem();
                        row.fileId = item.optString("fileId", "");
                        row.fileName = item.optString("fileName", "");
                        row.fileSize = item.optString("fileSize", "");
                        row.durationSec = item.optString("durationSec", "");
                        row.bitrateKbps = item.optString("bitrateKbps", "");
                        row.coverUrl = item.optString("coverUrl", "");
                        row.hlsUrl = item.optString("hlsUrl", "");
                        row.mp4DownloadUrl = item.optString("mp4DownloadUrl", "");
                        result.add(row);
                    }
                }
                runOnUiThread(() -> {
                    cloudVideos.clear();
                    cloudVideos.addAll(result);
                    tvHint.setText("云端视频数量: " + result.size());
                    adapter.notifyDataSetChanged();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    tvHint.setText("加载失败: " + e.getMessage());
                    Toast.makeText(this, "加载失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void openPlayer(CloudVideoItem item) {
        Intent intent = new Intent(this, CloudVideoPlayerActivity.class);
        intent.putExtra(CloudVideoPlayerActivity.EXTRA_FILE_ID, item.fileId);
        intent.putExtra(CloudVideoPlayerActivity.EXTRA_FILE_NAME, item.fileName);
        intent.putExtra(CloudVideoPlayerActivity.EXTRA_HLS_URL, item.hlsUrl);
        startActivity(intent);
    }

    @SuppressLint("SetTextI18n")
    private void downloadMp4(CloudVideoItem item) {
        ioExecutor.execute(() -> {
            try {
                String rawName = (item.fileName == null || item.fileName.trim().isEmpty()) ? "cloud_video.mp4" : item.fileName;
                final String safeName = rawName.replaceAll("[\\\\/:*?\"<>|]", "_");
                File targetFile = new File(MediaDemoConfig.getCloudDownloadDir(this), safeName);
                SimpleHttpClient.downloadToFile(item.mp4DownloadUrl, targetFile, (downloaded, total) -> runOnUiThread(() ->
                        tvHint.setText("下载中(" + safeName + "): " + downloaded + "/" + total)));
                runOnUiThread(() -> {
                    tvHint.setText("下载完成: " + targetFile.getAbsolutePath());
                    Toast.makeText(this, "下载完成: " + targetFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    tvHint.setText("下载失败: " + e.getMessage());
                    Toast.makeText(this, "下载失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private JSONObject requireResponseData(JSONObject response) {
        String code = response.optString("code", "400");
        if (!"200".equals(code)) {
            throw new IllegalStateException(response.optString("message", "request-failed"));
        }
        JSONObject data = response.optJSONObject("data");
        if (data == null) {
            throw new IllegalStateException("response-data-empty");
        }
        return data;
    }

    private class CloudVideoAdapter extends RecyclerView.Adapter<CloudVideoAdapter.CloudVideoViewHolder> {

        @NonNull
        @Override
        public CloudVideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cloud_video, parent, false);
            return new CloudVideoViewHolder(view);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull CloudVideoViewHolder holder, int position) {
            CloudVideoItem item = cloudVideos.get(position);
            holder.tvName.setText(item.fileName);
            holder.tvInfo.setText(
                    "大小: " + readableSizeMb(item.fileSize)
                            + "  |  时长: " + formatDuration(item.durationSec)
                            + "  |  码率: " + readableBitrate(item.bitrateKbps)
            );
            holder.btnPlay.setOnClickListener(v -> openPlayer(item));
            holder.btnDownload.setOnClickListener(v -> downloadMp4(item));
            holder.ivCover.setImageResource(android.R.color.darker_gray);
            if (item.coverUrl != null && !item.coverUrl.trim().isEmpty()) {
                ioExecutor.execute(() -> {
                    try (InputStream in = new URL(item.coverUrl).openStream()) {
                        Bitmap bitmap = BitmapFactory.decodeStream(in);
                        if (bitmap != null) {
                            runOnUiThread(() -> holder.ivCover.setImageBitmap(bitmap));
                        }
                    } catch (Exception ignored) {
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return cloudVideos.size();
        }

        class CloudVideoViewHolder extends RecyclerView.ViewHolder {
            private final ImageView ivCover;
            private final TextView tvName;
            private final TextView tvInfo;
            private final Button btnPlay;
            private final Button btnDownload;

            CloudVideoViewHolder(@NonNull View itemView) {
                super(itemView);
                ivCover = itemView.findViewById(R.id.ivCloudCover);
                tvName = itemView.findViewById(R.id.tvCloudVideoName);
                tvInfo = itemView.findViewById(R.id.tvCloudVideoInfo);
                btnPlay = itemView.findViewById(R.id.btnCloudPlay);
                btnDownload = itemView.findViewById(R.id.btnCloudDownload);
            }
        }

        private String readableSizeMb(String rawSize) {
            try {
                long bytes = Long.parseLong(rawSize);
                return String.format(Locale.getDefault(), "%.2fMB", bytes / 1024f / 1024f);
            } catch (Exception ignored) {
                return rawSize;
            }
        }

        private String formatDuration(String rawDurationSec) {
            try {
                double sec = Double.parseDouble(rawDurationSec);
                long total = Math.max(0L, (long) sec);
                long min = total / 60;
                long second = total % 60;
                return String.format(Locale.getDefault(), "%02d:%02d", min, second);
            } catch (Exception ignored) {
                return rawDurationSec + "s";
            }
        }

        private String readableBitrate(String rawBitrate) {
            try {
                long kbps = Long.parseLong(rawBitrate);
                return kbps + "kbps";
            } catch (Exception ignored) {
                return rawBitrate;
            }
        }
    }

    private static class CloudVideoItem {
        private String fileId;
        private String fileName;
        private String fileSize;
        private String durationSec;
        private String bitrateKbps;
        private String coverUrl;
        private String hlsUrl;
        private String mp4DownloadUrl;
    }

    @Override
    protected void onDestroy() {
        ioExecutor.shutdownNow();
        super.onDestroy();
    }
}
