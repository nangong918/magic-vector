package com.example.flutteraar.ui.activity;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.ui.PlayerView;

import com.example.flutteraar.R;

import java.util.List;

/**
 * Live 拉流播放 Demo：
 * 1) 支持 RTMP 拉流（依赖 media3-datasource-rtmp）；
 * 2) 支持 HLS 拉流（m3u8）；
 * 3) 支持 RTSP 拉流（依赖 media3-exoplayer-rtsp）；
 * 4) 支持把 RTMP 推流地址快速转换为 nginx HLS 预览地址。
 */
public class LivePullDemoActivity extends AppCompatActivity {
    private static final String TAG = "LivePullDemoActivity";

    private PlayerView playerView;
    private EditText editPullUrl;
    private TextView tvPullStatus;
    private Button btnPlayPull;
    private Button btnStopPull;
    private Button btnUseHls;
    private Button btnUseRtsp;

    private ExoPlayer player;
    private boolean pullPlaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_pull_demo);
        setTitle("Live Pull Demo");
        bindViews();
        initListeners();
    }

    private void bindViews() {
        playerView = findViewById(R.id.playerViewPull);
        editPullUrl = findViewById(R.id.editPullUrl);
        tvPullStatus = findViewById(R.id.tvPullStatus);
        btnPlayPull = findViewById(R.id.btnPlayPull);
        btnStopPull = findViewById(R.id.btnStopPull);
        btnUseHls = findViewById(R.id.btnUseHls);
        btnUseRtsp = findViewById(R.id.btnUseRtsp);

        editPullUrl.setText("rtmp://192.168.1.3:1935/stream/live");
        updateStatus("请输入 RTMP/HLS/RTSP 地址并开始播放");
        updateButtons();
    }

    private void initListeners() {
        btnPlayPull.setOnClickListener(v -> startPullPlay());
        btnStopPull.setOnClickListener(v -> stopPullPlay("已停止拉流播放"));
        btnUseHls.setOnClickListener(v -> fillHlsUrlFromRtmp());
        btnUseRtsp.setOnClickListener(v -> fillRtspUrlByCurrentHost());
    }

    @OptIn(markerClass = UnstableApi.class)
    private void startPullPlay() {
        String pullUrl = editPullUrl.getText().toString().trim();
        if (TextUtils.isEmpty(pullUrl)) {
            editPullUrl.setError("请输入 RTMP / HLS / RTSP 地址");
            return;
        }
        ensurePlayer();
        try {
            Uri uri = Uri.parse(pullUrl);
            if (isRtspUrl(pullUrl)) {
                applyRtspPlaybackPreference(true);
                RtspMediaSource mediaSource = new RtspMediaSource.Factory()
                        .setForceUseRtpTcp(true)
                        .createMediaSource(MediaItem.fromUri(uri));
                player.setMediaSource(mediaSource);
            } else {
                applyRtspPlaybackPreference(false);
                player.setMediaItem(MediaItem.fromUri(uri));
            }
            player.prepare();
            player.play();
            pullPlaying = true;
            Log.i(TAG, "startPullPlay, url=" + pullUrl);
            updateStatus("正在拉流: " + pullUrl);
            updateButtons();
        } catch (Exception e) {
            Log.e(TAG, "startPullPlay failed", e);
            pullPlaying = false;
            updateButtons();
            updateStatus("拉流启动失败: " + e.getMessage());
            Toast.makeText(this, "拉流启动失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopPullPlay(String message) {
        if (player != null) {
            player.stop();
            player.clearMediaItems();
        }
        pullPlaying = false;
        updateStatus(message);
        updateButtons();
    }

    @OptIn(markerClass = UnstableApi.class)
    private void ensurePlayer() {
        if (player != null) {
            return;
        }
        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this);
        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(dataSourceFactory))
                .build();
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) {
                    updateStatus("缓冲中...");
                } else if (playbackState == Player.STATE_READY) {
                    long liveOffsetMs = player.getCurrentLiveOffset();
                    String delay = liveOffsetMs == C.TIME_UNSET ? "未知" : (liveOffsetMs + "ms");
                    updateStatus("播放中，直播延迟: " + delay);
                } else if (playbackState == Player.STATE_ENDED) {
                    pullPlaying = false;
                    updateStatus("流已结束");
                    updateButtons();
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                pullPlaying = isPlaying;
                updateButtons();
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e(TAG, "onPlayerError", error);
                pullPlaying = false;
                updateButtons();
                updateStatus("播放失败: " + error.getMessage());
            }
        });
        playerView.setPlayer(player);
    }

    private void releasePlayer() {
        if (player == null) {
            return;
        }
        playerView.setPlayer(null);
        player.release();
        player = null;
        pullPlaying = false;
        updateButtons();
    }

    private void fillHlsUrlFromRtmp() {
        String pullUrl = editPullUrl.getText().toString().trim();
        if (!pullUrl.startsWith("rtmp://")) {
            Toast.makeText(this, "当前地址不是 RTMP，无法自动转换为 HLS", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = Uri.parse(pullUrl);
        String host = uri.getHost();
        List<String> pathSegments = uri.getPathSegments();
        if (TextUtils.isEmpty(host) || pathSegments == null || pathSegments.size() < 2) {
            Toast.makeText(this, "RTMP 地址格式不合法，示例: rtmp://ip:1935/stream/live", Toast.LENGTH_LONG).show();
            return;
        }
        String streamName = pathSegments.get(pathSegments.size() - 1);
        String hlsUrl = "http://" + host + ":8080/hls/" + streamName + "/index.m3u8";
        editPullUrl.setText(hlsUrl);
        editPullUrl.setSelection(hlsUrl.length());
        updateStatus("已转换为 HLS 地址");
    }

    private void fillRtspUrlByCurrentHost() {
        String pullUrl = editPullUrl.getText().toString().trim();
        String host = null;
        if (!TextUtils.isEmpty(pullUrl)) {
            Uri uri = Uri.parse(pullUrl);
            host = uri.getHost();
        }
        if (TextUtils.isEmpty(host)) {
            host = "192.168.1.3";
            Toast.makeText(this, "未识别到主机，已使用默认主机 192.168.1.3", Toast.LENGTH_SHORT).show();
        }
        String rtspUrl = "rtsp://" + host + ":8554/live/stream";
        editPullUrl.setText(rtspUrl);
        editPullUrl.setSelection(rtspUrl.length());
        updateStatus("已填写 RTSP 地址（MediaMTX 默认端口 8554）");
    }

    private boolean isRtspUrl(String url) {
        return url.startsWith("rtsp://");
    }

    private void applyRtspPlaybackPreference(boolean rtspMode) {
        if (player == null) {
            return;
        }
        TrackSelectionParameters params = player.getTrackSelectionParameters()
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, rtspMode)
                .build();
        player.setTrackSelectionParameters(params);
    }

    @SuppressLint("SetTextI18n")
    private void updateStatus(String message) {
        tvPullStatus.setText("状态: " + message);
    }

    private void updateButtons() {
        btnPlayPull.setEnabled(!pullPlaying);
        btnStopPull.setEnabled(pullPlaying);
    }

    @Override
    protected void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }
}
