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
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.flutteraar.R;
import com.example.flutteraar.media.MediaDemoConfig;
import com.example.flutteraar.media.SimpleHttpClient;
import com.example.flutteraar.media.worker.HlsToMp4Worker;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    private WorkManager workManager;
    private String hlsUrl;
    private String fileId;
    private String fileName;
    private String convertUniqueWorkName;
    private File localPlaylistFile;
    private ConvertUiState cloudConvertState;
    private String lastCompletedConvertWorkId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloud_video_player);
        setTitle("云上 HLS 播放");
        workManager = WorkManager.getInstance(this);
        bindViews();
        readExtras();
        if (isFinishing()) {
            return;
        }
        initPlayer();
        initActions();
        observeConvertState();
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
        convertUniqueWorkName = HlsToMp4Worker.buildUniqueWorkName(fileId);
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
        btnConvertMp4.setOnClickListener(v -> toggleConvertMp4Task());
    }

    private void observeConvertState() {
        workManager.getWorkInfosForUniqueWorkLiveData(convertUniqueWorkName).observe(this, workInfos -> {
            if (workInfos == null || workInfos.isEmpty()) {
                if (cloudConvertState == null || !cloudConvertState.isActive()) {
                    btnConvertMp4.setText("FFmpeg 转 MP4 并下载");
                }
                return;
            }
            WorkInfo workInfo = workInfos.get(0);
            cloudConvertState = ConvertUiState.from(workInfo);
            applyConvertUiState(cloudConvertState);
            if (cloudConvertState.isSuccess()) {
                String workId = workInfo.getId().toString();
                if (!workId.equals(lastCompletedConvertWorkId)) {
                    lastCompletedConvertWorkId = workId;
                    String outputPath = cloudConvertState.outputPath;
                    Toast.makeText(this, "转码完成: " + outputPath, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void applyConvertUiState(ConvertUiState state) {
        if (state.isActive()) {
            btnConvertMp4.setText("停止转码");
            if (state.total > 0) {
                int progress = (int) Math.min(100, (state.downloaded * 100 / state.total));
                updateStatus("FFmpeg 转 MP4 进行中: " + progress + "%  " + state.status);
            } else {
                updateStatus("FFmpeg 转 MP4 进行中: " + state.status);
            }
            return;
        }
        if (state.isSuccess()) {
            btnConvertMp4.setText("重新转码");
            updateStatus("FFmpeg 转 MP4完成: " + state.outputPath);
            return;
        }
        if (state.state == WorkInfo.State.CANCELLED) {
            btnConvertMp4.setText("继续转码");
            updateStatus("FFmpeg 转 MP4 已停止");
            return;
        }
        if (state.state == WorkInfo.State.FAILED) {
            btnConvertMp4.setText("重试转码");
            updateStatus("FFmpeg 转 MP4 失败: " + state.status);
            return;
        }
        btnConvertMp4.setText("FFmpeg 转 MP4 并下载");
    }

    private void toggleConvertMp4Task() {
        if (cloudConvertState != null && cloudConvertState.isActive()) {
            workManager.cancelUniqueWork(convertUniqueWorkName);
            Toast.makeText(this, "已停止转码任务", Toast.LENGTH_SHORT).show();
            return;
        }
        Data data = new Data.Builder()
                .putString(HlsToMp4Worker.KEY_FILE_ID, fileId)
                .putString(HlsToMp4Worker.KEY_FILE_NAME, fileName)
                .putString(HlsToMp4Worker.KEY_BASE_URL, MediaDemoConfig.SERVER_BASE_URL)
                .build();
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(HlsToMp4Worker.class)
                .setInputData(data)
                .setConstraints(constraints)
                .addTag(HlsToMp4Worker.TAG_CONVERT)
                .build();
        workManager.enqueueUniqueWork(convertUniqueWorkName, ExistingWorkPolicy.REPLACE, request);
        updateStatus("已提交 FFmpeg 转 MP4 任务...");
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
            return new ConvertUiState(
                    workInfo.getState(),
                    downloaded,
                    total,
                    status == null ? "" : status,
                    outputPath == null ? "" : outputPath
            );
        }

        private boolean isActive() {
            return state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED;
        }

        private boolean isSuccess() {
            return state == WorkInfo.State.SUCCEEDED;
        }
    }
}
