package com.example.flutteraar.ui.activity;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
    private Button btnPickFile;
    private Button btnStartPush;

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
        btnPickFile = findViewById(R.id.btnPickRtspInputFile);
        btnStartPush = findViewById(R.id.btnStartRtspPush);

        editInputPath.setText("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4");
        editRtspUrl.setText("rtsp://192.168.1.3:8554/live/stream");
        updateStatus("请输入输入源与 RTSP 推流地址");
    }

    private void initListeners() {
        btnPickFile.setOnClickListener(v -> pickMp4Launcher.launch(new String[]{"video/mp4", "video/*"}));
        btnStartPush.setOnClickListener(v -> startPush());
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
}
