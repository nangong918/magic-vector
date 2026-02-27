package com.example.flutteraar.ui.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.demo.aarlib.voicewakeup.VoiceWakeUpBridge;
import com.example.flutteraar.R;
import com.example.flutteraar.config.ModuleKeyConfigLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class IvwDemoActivity extends AppCompatActivity {
    private static final int REQ_RECORD_AUDIO = 0x66;

    private EditText etKeyword;
    private TextView tvState;
    private TextView tvDb;
    private TextView tvFilePath;
    private TextView tvLog;
    private ScrollView svLog;
    private String demoAudioPath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ivw_demo);
        setTitle("离线唤醒 Demo");
        bindViews();
        applyDefaultConfig();
        bindActions();
        prepareDemoAudioFile();
    }

    private void bindViews() {
        etKeyword = findViewById(R.id.etKeyword);
        tvState = findViewById(R.id.tvState);
        tvDb = findViewById(R.id.tvDb);
        tvFilePath = findViewById(R.id.tvFilePath);
        tvLog = findViewById(R.id.tvLog);
        svLog = findViewById(R.id.svLog);
    }

    private void bindActions() {
        Button btnInit = findViewById(R.id.btnInitSdk);
        Button btnStartRecord = findViewById(R.id.btnStartRecordWake);
        Button btnStopRecord = findViewById(R.id.btnStopRecordWake);
        Button btnStartFile = findViewById(R.id.btnStartFileWake);
        Button btnRelease = findViewById(R.id.btnReleaseSdk);
        Button btnClearLog = findViewById(R.id.btnClearLog);

        btnInit.setOnClickListener(v -> {
            applyDefaultConfig();
            VoiceWakeUpBridge.initSdk(this);
        });
        btnStartRecord.setOnClickListener(v -> startRecordWakeWithPermission());
        btnStopRecord.setOnClickListener(v -> VoiceWakeUpBridge.stopRecordWake());
        btnStartFile.setOnClickListener(v -> {
            if (TextUtils.isEmpty(demoAudioPath)) {
                appendLog("测试音频不存在，请检查assets/ivw/AudioCache/test.pcm");
                return;
            }
            VoiceWakeUpBridge.startFileWake(this, getKeyword(), demoAudioPath);
        });
        btnRelease.setOnClickListener(v -> VoiceWakeUpBridge.release());
        btnClearLog.setOnClickListener(v -> tvLog.setText(""));
    }

    private void prepareDemoAudioFile() {
        try {
            File baseDir = getExternalFilesDir(null);
            if (baseDir == null) {
                baseDir = getFilesDir();
            }
            File outDir = new File(baseDir, "ivw_demo");
            if (!outDir.exists()) {
                outDir.mkdirs();
            }
            File outFile = new File(outDir, "test.pcm");
            copyAssetToFile("ivw/AudioCache/test.pcm", outFile);
            demoAudioPath = outFile.getAbsolutePath();
            tvFilePath.setText("文件唤醒音频: " + demoAudioPath);
        } catch (Exception e) {
            appendLog("准备测试音频失败: " + e.getMessage());
            tvFilePath.setText("文件唤醒音频: 不可用");
        }
    }

    private void copyAssetToFile(String assetPath, File outFile) throws IOException {
        try (InputStream input = getAssets().open(assetPath);
             FileOutputStream output = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) > 0) {
                output.write(buffer, 0, count);
            }
            output.flush();
        }
    }

    private String getKeyword() {
        String keyword = etKeyword.getText().toString().trim();
        return TextUtils.isEmpty(keyword) ? "你好小迪" : keyword;
    }

    private void startRecordWakeWithPermission() {
        if (VoiceWakeUpBridge.hasRecordPermission(this)) {
            VoiceWakeUpBridge.startRecordWake(this, getKeyword());
            return;
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
    }

    private void applyDefaultConfig() {
        VoiceWakeUpBridge.Config config = ModuleKeyConfigLoader.loadOfflineIvwConfig(this);
        if (config != null) {
            VoiceWakeUpBridge.setDefaultConfig(config);
        } else {
            appendLog("读取module_key.json失败或offlineIvw字段不完整");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        VoiceWakeUpBridge.setEventListener(this::onVoiceEvent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        VoiceWakeUpBridge.clearEventListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VoiceWakeUpBridge.release();
    }

    private void onVoiceEvent(Map<String, Object> payload) {
        runOnUiThread(() -> {
            String type = String.valueOf(payload.get("type"));
            String message = String.valueOf(payload.get("message"));
            appendLog("[" + type + "] " + message);

            if ("db".equals(type)) {
                Object dbObj = payload.get("db");
                tvDb.setText("当前分贝: " + (dbObj == null ? "-" : dbObj));
            }
            if ("state".equals(type)) {
                Object stateObj = payload.get("state");
                tvState.setText("会话状态: " + (stateObj == null ? "-" : stateObj));
            }
            if ("auth".equals(type)) {
                tvState.setText(message);
            }
        });
    }

    private void appendLog(String text) {
        tvLog.append(text);
        tvLog.append("\n");
        svLog.post(() -> svLog.fullScroll(ScrollView.FOCUS_DOWN));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_RECORD_AUDIO) {
            return;
        }
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (granted) {
            VoiceWakeUpBridge.startRecordWake(this, getKeyword());
        } else {
            appendLog("录音权限被拒绝");
        }
    }
}
