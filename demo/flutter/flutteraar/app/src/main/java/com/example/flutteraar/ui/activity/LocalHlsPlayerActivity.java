package com.example.flutteraar.ui.activity;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.example.flutteraar.R;

import java.io.File;

public class LocalHlsPlayerActivity extends AppCompatActivity {
    public static final String EXTRA_PLAYLIST_PATH = "extra_playlist_path";

    private PlayerView playerView;
    private TextView tvStatus;
    private ExoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_hls_player);
        setTitle("本地 HLS 缓存播放");
        playerView = findViewById(R.id.playerViewLocalHls);
        tvStatus = findViewById(R.id.tvLocalHlsStatus);

        String playlistPath = getIntent().getStringExtra(EXTRA_PLAYLIST_PATH);
        if (TextUtils.isEmpty(playlistPath)) {
            Toast.makeText(this, "播放参数缺失", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        File playlistFile = new File(playlistPath);
        if (!playlistFile.exists()) {
            Toast.makeText(this, "缓存播放单不存在", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) {
                    tvStatus.setText("状态: 缓冲中...");
                } else if (playbackState == Player.STATE_READY) {
                    tvStatus.setText("状态: 播放中");
                } else if (playbackState == Player.STATE_ENDED) {
                    tvStatus.setText("状态: 播放结束");
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                tvStatus.setText("状态: 播放失败 - " + error.getMessage());
            }
        });
        tvStatus.setText("状态: " + playlistFile.getAbsolutePath());
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(playlistFile)));
        player.prepare();
        player.play();
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}
