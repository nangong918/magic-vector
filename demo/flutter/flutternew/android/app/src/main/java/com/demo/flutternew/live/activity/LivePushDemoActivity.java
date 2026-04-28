package com.demo.flutternew.live.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.demo.aarlib.live.LiveErrorCode;
import com.demo.aarlib.live.LiveFrameFormat;
import com.demo.aarlib.live.LivePushConfig;
import com.demo.aarlib.live.LivePushListener;
import com.demo.aarlib.live.LivePusherBridge;
import com.demo.aarlib.live.ffmpeg.FFmpegPushBridge;
import com.demo.flutternew.R;
import com.demo.flutternew.live.camera.Camera2Helper;
import com.demo.flutternew.live.camera.Camera2Listener;

import java.util.Map;

public class LivePushDemoActivity extends AppCompatActivity implements Camera2Listener, LivePushListener {
    private static final String TAG = "LivePushDemoActivity";
    private static final int VIDEO_BITRATE = 800_000;
    private static final int VIDEO_FRAME_RATE = 10;
    private static final int AUDIO_SAMPLE_RATE = 44_100;
    private static final int AUDIO_CHANNELS = 2;

    private TextureView texturePreview;
    private EditText editLiveUrl;
    private EditText editInputPath;
    private EditText editFilePushUrl;
    private TextView tvStatus;
    private Button btnTogglePush;
    private Button btnSwitchCamera;
    private Button btnPushFile;
    private ToggleButton btnMute;

    private Camera2Helper camera2Helper;
    private LivePusherBridge livePusherBridge;
    private AudioCaptureTask audioCaptureTask;
    private Size previewSize;
    private int previewDegree = 90;
    private boolean pushing;
    private boolean permissionsGranted;
    private OrientationEventListener orientationListener;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onPermissionResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_push_demo);
        setTitle("Live Push Demo (Android)");
        bindViews();
        initListeners();
        initOrientationListener();
        ensurePermissions();
    }

    private void bindViews() {
        texturePreview = findViewById(R.id.texturePreview);
        editLiveUrl = findViewById(R.id.editLiveUrl);
        editInputPath = findViewById(R.id.editInputPath);
        editFilePushUrl = findViewById(R.id.editFilePushUrl);
        tvStatus = findViewById(R.id.tvStatus);
        btnTogglePush = findViewById(R.id.btnTogglePush);
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera);
        btnPushFile = findViewById(R.id.btnPushFile);
        btnMute = findViewById(R.id.btnMute);

        editLiveUrl.setText("rtmp://192.168.1.3:1935/stream/live");
        editInputPath.setText("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4");
        editFilePushUrl.setText("rtmp://192.168.1.3:1935/stream/live");
        btnMute.setChecked(false);
        updateStatus("等待权限与相机初始化");
        updateButtons();
    }

    @SuppressLint("MissingPermission")
    private void initListeners() {
        btnTogglePush.setOnClickListener(v -> {
            if (pushing) {
                stopLivePush();
            } else {
                startLivePush();
            }
        });
        btnSwitchCamera.setOnClickListener(v -> {
            if (camera2Helper == null) {
                return;
            }
            if (pushing) {
                stopLivePush();
            }
            camera2Helper.switchCamera();
            updateStatus("已切换摄像头");
        });
        btnMute.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (livePusherBridge != null) {
                livePusherBridge.setMute(isChecked);
            }
        });
        btnPushFile.setOnClickListener(v -> startFilePush());
    }

    private void initOrientationListener() {
        orientationListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                int newPreviewDegree = mapPreviewDegree(orientation);
                if (newPreviewDegree < 0 || newPreviewDegree == previewDegree) {
                    return;
                }
                Log.i(TAG, "onOrientationChanged, previewDegree=" + newPreviewDegree);
                previewDegree = newPreviewDegree;
                if (camera2Helper != null) {
                    camera2Helper.updatePreviewDegree(newPreviewDegree);
                }
                if (livePusherBridge != null && previewSize != null) {
                    int width = previewSize.getWidth();
                    int height = previewSize.getHeight();
                    if (newPreviewDegree == 90 || newPreviewDegree == 270) {
                        int temp = width;
                        width = height;
                        height = temp;
                    }
                    livePusherBridge.updateVideoCodecInfo(width, height);
                }
            }
        };
    }

    private void ensurePermissions() {
        boolean cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        boolean audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        permissionsGranted = cameraGranted && audioGranted;
        Log.i(TAG, "ensurePermissions, cameraGranted=" + cameraGranted + ", audioGranted=" + audioGranted);
        if (permissionsGranted) {
            initCameraPreview();
        } else {
            permissionLauncher.launch(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO});
        }
    }

    private void onPermissionResult(Map<String, Boolean> result) {
        boolean granted = true;
        for (Boolean value : result.values()) {
            if (!Boolean.TRUE.equals(value)) {
                granted = false;
                break;
            }
        }
        permissionsGranted = granted;
        Log.i(TAG, "onPermissionResult, granted=" + granted);
        if (granted) {
            initCameraPreview();
        } else {
            updateStatus("缺少 CAMERA / RECORD_AUDIO 权限");
            Toast.makeText(this, "请先授予相机和录音权限", Toast.LENGTH_LONG).show();
            updateButtons();
        }
    }

    private void initCameraPreview() {
        if (camera2Helper != null) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        camera2Helper = new Camera2Helper.Builder()
                .context(getApplicationContext())
                .cameraListener(this)
                .previewOn(texturePreview)
                .previewViewSize(new Point(640, 480))
                .specificCameraId(Camera2Helper.CAMERA_ID_BACK)
                .rotation(rotation)
                .rotateDegree(getPreviewDegree(rotation))
                .build();
        Log.i(TAG, "initCameraPreview, rotation=" + rotation);
        camera2Helper.start();
        updateStatus("相机初始化中");
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private void startLivePush() {
        if (!permissionsGranted) {
            ensurePermissions();
            return;
        }
        if (previewSize == null) {
            updateStatus("相机尚未就绪，请稍后再试");
            return;
        }
        String liveUrl = editLiveUrl.getText().toString().trim();
        if (liveUrl.isEmpty()) {
            editLiveUrl.setError("请输入 RTMP 地址");
            return;
        }
        stopLivePush();

        int width = previewSize.getWidth();
        int height = previewSize.getHeight();
        if (previewDegree == 90 || previewDegree == 270) {
            int temp = width;
            width = height;
            height = temp;
        }
        LivePushConfig config = new LivePushConfig(
                width,
                height,
                VIDEO_BITRATE,
                VIDEO_FRAME_RATE,
                AUDIO_SAMPLE_RATE,
                AUDIO_CHANNELS);
        livePusherBridge = new LivePusherBridge(config, this);
        livePusherBridge.setMute(btnMute.isChecked());
        livePusherBridge.startPush(liveUrl);
        Log.i(TAG, "startLivePush, url=" + liveUrl + ", width=" + width + ", height=" + height);
        audioCaptureTask = new AudioCaptureTask(livePusherBridge);
        audioCaptureTask.start();
        pushing = true;
        updateStatus("实时推流已启动");
        updateButtons();
    }

    private void stopLivePush() {
        if (audioCaptureTask != null) {
            audioCaptureTask.stop();
            audioCaptureTask = null;
        }
        if (livePusherBridge != null) {
            livePusherBridge.stopPush();
            livePusherBridge.release();
            livePusherBridge = null;
        }
        if (pushing) {
            Log.i(TAG, "stopLivePush");
            updateStatus("实时推流已停止");
        }
        pushing = false;
        updateButtons();
    }

    private void startFilePush() {
        String inputPath = editInputPath.getText().toString().trim();
        String liveUrl = editFilePushUrl.getText().toString().trim();
        if (inputPath.isEmpty()) {
            editInputPath.setError("请输入媒体地址或文件路径");
            return;
        }
        if (liveUrl.isEmpty()) {
            editFilePushUrl.setError("请输入 RTMP 地址");
            return;
        }
        btnPushFile.setEnabled(false);
        updateStatus("开始 FFmpeg 文件推流");
        Log.i(TAG, "startFilePush, inputPath=" + inputPath + ", url=" + liveUrl);
        FFmpegPushBridge.pushStreamAsync(inputPath, liveUrl, (resultCode, message) -> {
            btnPushFile.setEnabled(true);
            Log.i(TAG, "startFilePush callback, resultCode=" + resultCode + ", message=" + message);
            updateStatus(message);
        });
    }

    private void updateButtons() {
        btnTogglePush.setEnabled(permissionsGranted && previewSize != null);
        btnTogglePush.setText(pushing ? "停止实时推流" : "开始实时推流");
        btnMute.setEnabled(pushing);
    }

    private void updateStatus(String message) {
        tvStatus.setText("状态: " + message);
    }

    private int getPreviewDegree(int rotation) {
        switch (rotation) {
            case Surface.ROTATION_0:
                return 90;
            case Surface.ROTATION_90:
                return 0;
            case Surface.ROTATION_180:
                return 270;
            case Surface.ROTATION_270:
                return 180;
            default:
                return 90;
        }
    }

    private int mapPreviewDegree(int orientation) {
        if (orientation < 0) {
            return -1;
        }
        if (orientation >= 315 || orientation < 45) {
            return 90;
        }
        if (orientation < 135) {
            return 0;
        }
        if (orientation < 225) {
            return 270;
        }
        return 180;
    }

    @Override
    public void onCameraOpened(@NonNull Size previewSize, int displayOrientation) {
        this.previewSize = previewSize;
        this.previewDegree = displayOrientation;
        runOnUiThread(() -> {
            Log.i(TAG, "onCameraOpened, previewSize=" + previewSize + ", displayOrientation=" + displayOrientation);
            updateStatus("相机已就绪: " + previewSize.getWidth() + "x" + previewSize.getHeight());
            updateButtons();
        });
    }

    @Override
    public void onPreviewFrame(byte[] yuvData) {
        if (!pushing || livePusherBridge == null) {
            return;
        }
        try {
            livePusherBridge.pushVideoFrame(yuvData, LiveFrameFormat.I420);
        } catch (IllegalStateException ignored) {
            Log.w(TAG, "onPreviewFrame ignored, push state invalid");
        }
    }

    @Override
    public void onCameraClosed() {
        runOnUiThread(() -> {
            Log.i(TAG, "onCameraClosed");
            previewSize = null;
            updateStatus("相机已关闭");
            updateButtons();
        });
    }

    @Override
    public void onCameraError(Exception e) {
        runOnUiThread(() -> {
            Log.e(TAG, "onCameraError", e);
            updateStatus("相机异常: " + e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onError(int errorCode, String message) {
        runOnUiThread(() -> {
            Log.e(TAG, "onError, code=" + errorCode + ", message=" + message);
            stopLivePush();
            String detail = "Live SDK 错误(" + errorCode + "): " + message;
            if (errorCode == LiveErrorCode.ERROR_RTMP_CONNECT_SERVER) {
                detail = "RTMP 服务连接失败";
            } else if (errorCode == LiveErrorCode.ERROR_RTMP_CONNECT_STREAM) {
                detail = "RTMP Stream 连接失败";
            }
            updateStatus(detail);
            Toast.makeText(this, detail, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected void onDestroy() {
        stopLivePush();
        if (orientationListener != null) {
            orientationListener.disable();
        }
        if (camera2Helper != null) {
            camera2Helper.release();
            camera2Helper = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (orientationListener != null && orientationListener.canDetectOrientation()) {
            orientationListener.enable();
        }
    }

    @Override
    protected void onPause() {
        if (orientationListener != null) {
            orientationListener.disable();
        }
        super.onPause();
    }

    private final class AudioCaptureTask implements Runnable {
        private final LivePusherBridge bridge;
        private final AudioRecord audioRecord;
        private final int frameBytes;
        private volatile boolean running;
        private Thread worker;

        @RequiresPermission(Manifest.permission.RECORD_AUDIO)
        private AudioCaptureTask(LivePusherBridge bridge) {
            this.bridge = bridge;
            int channelConfig = AUDIO_CHANNELS == 2
                    ? AudioFormat.CHANNEL_IN_STEREO
                    : AudioFormat.CHANNEL_IN_MONO;
            frameBytes = Math.max(bridge.getAudioInputByteCount(), 2048);
            int minBufferSize = AudioRecord.getMinBufferSize(
                    AUDIO_SAMPLE_RATE,
                    channelConfig,
                    AudioFormat.ENCODING_PCM_16BIT);
            int bufferSize = Math.max(minBufferSize, frameBytes);
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    AUDIO_SAMPLE_RATE,
                    channelConfig,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize);
            Log.i(TAG, "AudioCaptureTask init, frameBytes=" + frameBytes + ", bufferSize=" + bufferSize);
        }

        private void start() {
            running = true;
            worker = new Thread(this, "live-audio-capture");
            worker.start();
            Log.i(TAG, "AudioCaptureTask start");
        }

        private void stop() {
            running = false;
            if (worker != null) {
                worker.interrupt();
            }
            try {
                audioRecord.stop();
            } catch (IllegalStateException ignored) {
            }
            audioRecord.release();
            Log.i(TAG, "AudioCaptureTask stop");
        }

        @Override
        public void run() {
            byte[] buffer = new byte[frameBytes];
            try {
                audioRecord.startRecording();
                Log.i(TAG, "AudioCaptureTask recording started");
                while (running && pushing) {
                    int len = audioRecord.read(buffer, 0, buffer.length);
                    if (len <= 0) {
                        continue;
                    }
                    if (len == buffer.length) {
                        bridge.pushAudioFrame(buffer.clone());
                    } else {
                        byte[] exact = new byte[len];
                        System.arraycopy(buffer, 0, exact, 0, len);
                        bridge.pushAudioFrame(exact);
                    }
                }
            } catch (IllegalStateException ignored) {
                Log.w(TAG, "AudioCaptureTask run interrupted by IllegalStateException");
            }
        }
    }
}
