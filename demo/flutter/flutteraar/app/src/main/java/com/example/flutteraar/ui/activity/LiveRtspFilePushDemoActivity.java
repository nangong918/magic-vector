package com.example.flutteraar.ui.activity;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.demo.aarlib.live.ffmpeg.FFmpegPushBridge;
import com.example.flutteraar.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * RTSP 文件推流 Demo：
 * - 输入本地文件或网络媒体源；
 * - 使用 FFmpeg 转推到 RTSP 地址；
 * - 独立于原有 RTMP Demo，避免影响已验证链路。
 */
public class LiveRtspFilePushDemoActivity extends AppCompatActivity {
    private static final String TAG = "LiveRtspFilePushDemo";

    private EditText editInputPath;
    private EditText editRtspUrl;
    private TextView tvStatus;
    private TextView tvPreviewProgress;
    private Button btnPickFile;
    private Button btnTogglePreview;
    private Button btnStartPush;
    private VideoView videoPreview;
    private SeekBar seekPreview;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressSyncTask = new Runnable() {
        @Override
        public void run() {
            syncPreviewProgress();
            uiHandler.postDelayed(this, 300);
        }
    };
    private long previewDurationMs;
    private boolean userSeeking;
    private String previewSourcePath;

    private final ActivityResultLauncher<String[]> pickMp4Launcher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onMp4Picked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_rtsp_file_push_demo);
        setTitle("RTSP File Push Demo");
        bindViews();
        initListeners();
    }

    private void bindViews() {
        editInputPath = findViewById(R.id.editRtspInputPath);
        editRtspUrl = findViewById(R.id.editRtspOutputUrl);
        tvStatus = findViewById(R.id.tvRtspPushStatus);
        tvPreviewProgress = findViewById(R.id.tvRtspPreviewProgress);
        btnPickFile = findViewById(R.id.btnPickRtspInputFile);
        btnTogglePreview = findViewById(R.id.btnTogglePreview);
        btnStartPush = findViewById(R.id.btnStartRtspPush);
        videoPreview = findViewById(R.id.videoRtspPreview);
        seekPreview = findViewById(R.id.seekRtspPreview);

        editInputPath.setText("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4");
        editRtspUrl.setText("rtsp://192.168.1.3:8554/live/stream");
        updatePreviewProgressText(0, 0);
        updateStatus("请输入输入源与 RTSP 推流地址");
        seekPreview.setEnabled(false);
        btnTogglePreview.setEnabled(false);
    }

    private void initListeners() {
        btnPickFile.setOnClickListener(v -> pickMp4Launcher.launch(new String[]{"video/mp4", "video/*"}));
        btnStartPush.setOnClickListener(v -> startPush());
        btnTogglePreview.setOnClickListener(v -> togglePreviewPlayback());
        seekPreview.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    updatePreviewProgressText(progress, previewDurationMs);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                userSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                userSeeking = false;
                if (previewSourcePath == null) {
                    return;
                }
                videoPreview.seekTo(seekBar.getProgress());
                syncPreviewProgress();
            }
        });
    }

    private void onMp4Picked(Uri uri) {
        if (uri == null) {
            updateStatus("未选择文件");
            return;
        }
        btnPickFile.setEnabled(false);
        btnStartPush.setEnabled(false);
        updateStatus("正在准备本地文件...");
        new Thread(() -> {
            try {
                String localPath = copyUriToCacheFile(uri);
                runOnUiThread(() -> {
                    editInputPath.setText(localPath);
                    editInputPath.setSelection(localPath.length());
                    attachPreviewSource(localPath);
                    btnPickFile.setEnabled(true);
                    btnStartPush.setEnabled(true);
                    updateStatus("已选择文件: " + localPath);
                });
            } catch (IOException e) {
                Log.e(TAG, "copyUriToCacheFile failed", e);
                runOnUiThread(() -> {
                    btnPickFile.setEnabled(true);
                    btnStartPush.setEnabled(true);
                    updateStatus("文件准备失败: " + e.getMessage());
                    Toast.makeText(this, "文件准备失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }, "rtsp-file-prepare").start();
    }

    private String copyUriToCacheFile(Uri uri) throws IOException {
        File targetFile = new File(getCacheDir(), "rtsp_push_" + System.currentTimeMillis() + ".mp4");
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new IOException("无法读取所选文件");
            }
            try (FileOutputStream out = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[8 * 1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.flush();
            }
        }
        return targetFile.getAbsolutePath();
    }

    private void attachPreviewSource(String path) {
        previewSourcePath = path;
        previewDurationMs = 0;
        seekPreview.setProgress(0);
        seekPreview.setEnabled(false);
        btnTogglePreview.setEnabled(true);
        btnTogglePreview.setText("播放预览");
        updatePreviewProgressText(0, 0);

        videoPreview.setVideoPath(path);
        videoPreview.setOnPreparedListener(mediaPlayer -> {
            previewDurationMs = videoPreview.getDuration();
            if (previewDurationMs < 0) {
                previewDurationMs = 0;
            }
            seekPreview.setMax((int) previewDurationMs);
            seekPreview.setEnabled(previewDurationMs > 0);
            syncPreviewProgress();
        });
        videoPreview.setOnCompletionListener(mediaPlayer -> {
            btnTogglePreview.setText("播放预览");
            syncPreviewProgress();
        });
    }

    private void togglePreviewPlayback() {
        if (previewSourcePath == null) {
            Toast.makeText(this, "请先选择本地 mp4 文件", Toast.LENGTH_SHORT).show();
            return;
        }
        if (videoPreview.isPlaying()) {
            videoPreview.pause();
            btnTogglePreview.setText("播放预览");
        } else {
            videoPreview.start();
            btnTogglePreview.setText("暂停预览");
        }
        syncPreviewProgress();
    }

    private void syncPreviewProgress() {
        if (previewSourcePath == null) {
            updatePreviewProgressText(0, 0);
            return;
        }
        int current = videoPreview.getCurrentPosition();
        if (!userSeeking) {
            seekPreview.setProgress(current);
        }
        updatePreviewProgressText(current, previewDurationMs);
    }

    private void updatePreviewProgressText(long currentMs, long totalMs) {
        tvPreviewProgress.setText("预览进度: " + formatTime(currentMs) + " / " + formatTime(totalMs));
    }

    private String formatTime(long timeMs) {
        long totalSeconds = Math.max(0, timeMs / 1000);
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void startPush() {
        String inputPath = editInputPath.getText().toString().trim();
        String outputUrl = editRtspUrl.getText().toString().trim();

        if (TextUtils.isEmpty(inputPath)) {
            editInputPath.setError("请输入媒体文件路径或网络地址");
            return;
        }
        if (TextUtils.isEmpty(outputUrl)) {
            editRtspUrl.setError("请输入 RTSP 地址");
            return;
        }
        if (!outputUrl.startsWith("rtsp://")) {
            editRtspUrl.setError("RTSP 地址需以 rtsp:// 开头");
            return;
        }

        if (!TextUtils.equals(previewSourcePath, inputPath)) {
            attachPreviewSource(inputPath);
        }
        videoPreview.seekTo(0);
        videoPreview.start();
        btnTogglePreview.setText("暂停预览");
        btnStartPush.setEnabled(false);
        updateStatus("RTSP 文件推流中...");
        Log.i(TAG, "startPush, input=" + inputPath + ", output=" + outputUrl);
        FFmpegPushBridge.pushStreamAsync(inputPath, outputUrl, (resultCode, message) -> {
            btnStartPush.setEnabled(true);
            String finalMessage = resultCode >= 0
                    ? "RTSP 推流完成: " + message
                    : "RTSP 推流失败, code=" + resultCode;
            Log.i(TAG, "push callback, code=" + resultCode + ", message=" + message);
            updateStatus(finalMessage);
            Toast.makeText(this, finalMessage, Toast.LENGTH_LONG).show();
        });
    }

    private void updateStatus(String message) {
        tvStatus.setText("状态: " + message);
    }

    @Override
    protected void onResume() {
        super.onResume();
        uiHandler.post(progressSyncTask);
    }

    @Override
    protected void onPause() {
        uiHandler.removeCallbacks(progressSyncTask);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        uiHandler.removeCallbacks(progressSyncTask);
        if (videoPreview != null) {
            videoPreview.stopPlayback();
        }
        super.onDestroy();
    }
}
