package com.example.flutteraar.ui.activity;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.example.flutteraar.R;
import com.example.flutteraar.media.MediaDemoConfig;
import com.example.flutteraar.media.SimpleHttpClient;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CloudVideoPlayerActivity extends AppCompatActivity {
    public static final String EXTRA_HLS_URL = "extra_hls_url";
    public static final String EXTRA_FILE_ID = "extra_file_id";
    public static final String EXTRA_FILE_NAME = "extra_file_name";

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private PlayerView playerView;
    private TextView tvStatus;
    private Button btnSaveHls;
    private Button btnPlayLocalHls;
    private Button btnConvertMp4;

    private ExoPlayer player;
    private String hlsUrl;
    private String fileId;
    private String fileName;
    private File localPlaylistFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloud_video_player);
        setTitle("云上 HLS 播放");
        bindViews();
        readExtras();
        initPlayer();
        initActions();
    }

    private void bindViews() {
        playerView = findViewById(R.id.playerViewCloud);
        tvStatus = findViewById(R.id.tvCloudPlayerStatus);
        btnSaveHls = findViewById(R.id.btnSaveHlsLocal);
        btnPlayLocalHls = findViewById(R.id.btnPlayLocalHls);
        btnConvertMp4 = findViewById(R.id.btnConvertHlsMp4);
        btnPlayLocalHls.setEnabled(false);
    }

    private void readExtras() {
        hlsUrl = getIntent().getStringExtra(EXTRA_HLS_URL);
        fileId = getIntent().getStringExtra(EXTRA_FILE_ID);
        fileName = getIntent().getStringExtra(EXTRA_FILE_NAME);
        if (TextUtils.isEmpty(hlsUrl) || TextUtils.isEmpty(fileId)) {
            Toast.makeText(this, "播放参数缺失", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        updateStatus("准备播放: " + hlsUrl);
    }

    private void initPlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) {
                    updateStatus("缓冲中...");
                } else if (playbackState == Player.STATE_READY) {
                    updateStatus("播放中");
                } else if (playbackState == Player.STATE_ENDED) {
                    updateStatus("播放结束");
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                updateStatus("播放失败: " + error.getMessage());
            }
        });
        playUrl(hlsUrl);
    }

    private void initActions() {
        btnSaveHls.setOnClickListener(v -> cacheHlsToLocal());
        btnPlayLocalHls.setOnClickListener(v -> {
            if (localPlaylistFile != null && localPlaylistFile.exists()) {
                playUrl(Uri.fromFile(localPlaylistFile).toString());
            }
        });
        btnConvertMp4.setOnClickListener(v -> convertAndDownloadMp4());
    }

    private void playUrl(String url) {
        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)));
        player.prepare();
        player.play();
    }

    private void cacheHlsToLocal() {
        btnSaveHls.setEnabled(false);
        ioExecutor.execute(() -> {
            try {
                String playlist = SimpleHttpClient.getText(hlsUrl);
                File root = new File(MediaDemoConfig.getHlsCacheDir(this), "file_" + fileId);
                if (!root.exists()) {
                    root.mkdirs();
                }
                File segmentDir = new File(root, "segments");
                if (!segmentDir.exists()) {
                    segmentDir.mkdirs();
                }
                List<String> lines = readLines(playlist);
                List<String> outputLines = new ArrayList<>();
                int segmentIndex = 0;
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        outputLines.add(line);
                        continue;
                    }
                    String absoluteSegmentUrl = URI.create(hlsUrl).resolve(trimmed).toString();
                    String segmentName = String.format(Locale.getDefault(), "seg_%05d.ts", segmentIndex++);
                    File segmentFile = new File(segmentDir, segmentName);
                    SimpleHttpClient.downloadToFile(absoluteSegmentUrl, segmentFile, null);
                    outputLines.add("segments/" + segmentName);
                }
                localPlaylistFile = new File(root, "index_local.m3u8");
                try (FileOutputStream out = new FileOutputStream(localPlaylistFile)) {
                    String content = String.join("\n", outputLines) + "\n";
                    out.write(content.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                }
                runOnUiThread(() -> {
                    btnPlayLocalHls.setEnabled(true);
                    updateStatus("已缓存 HLS: " + localPlaylistFile.getAbsolutePath());
                    btnSaveHls.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnSaveHls.setEnabled(true);
                    updateStatus("缓存 HLS 失败: " + e.getMessage());
                    Toast.makeText(this, "缓存失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void convertAndDownloadMp4() {
        btnConvertMp4.setEnabled(false);
        ioExecutor.execute(() -> {
            try {
                Map<String, String> form = new HashMap<>();
                form.put("fileId", fileId);
                JSONObject response = SimpleHttpClient.postForm(
                        MediaDemoConfig.SERVER_BASE_URL + "/video/cloud/hls/to-mp4",
                        form
                );
                JSONObject data = requireResponseData(response);
                String downloadUrl = data.optString("downloadUrl", "");
                if (downloadUrl.isEmpty()) {
                    throw new IllegalStateException("downloadUrl empty");
                }
                String safeName = (fileName == null || fileName.trim().isEmpty()) ? "cloud_video" : fileName;
                safeName = safeName.replaceAll("[\\\\/:*?\"<>|]", "_");
                File target = new File(MediaDemoConfig.getCloudDownloadDir(this), safeName + "_hls.mp4");
                SimpleHttpClient.downloadToFile(downloadUrl, target, (downloaded, total) -> runOnUiThread(() ->
                        updateStatus("FFmpeg 转 MP4 下载中: " + downloaded + "/" + total)));
                runOnUiThread(() -> {
                    updateStatus("FFmpeg 转 MP4完成: " + target.getAbsolutePath());
                    btnConvertMp4.setEnabled(true);
                    Toast.makeText(this, "已下载: " + target.getAbsolutePath(), Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnConvertMp4.setEnabled(true);
                    updateStatus("FFmpeg 转 MP4 失败: " + e.getMessage());
                    Toast.makeText(this, "转换失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

    private List<String> readLines(String text) {
        List<String> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                lines.add(text.substring(start, i).replace("\r", ""));
                start = i + 1;
            }
        }
        if (start < text.length()) {
            lines.add(text.substring(start).replace("\r", ""));
        }
        return lines;
    }

    private void updateStatus(String message) {
        tvStatus.setText("状态: " + message);
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
        ioExecutor.shutdownNow();
        super.onDestroy();
    }
}
