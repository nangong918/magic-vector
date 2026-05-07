package com.example.flutteraar.ui.activity;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.flutteraar.R;

import java.io.File;

public class LocalVideoPlayerActivity extends AppCompatActivity {
    public static final String EXTRA_FILE_PATH = "extra_file_path";

    private VideoView videoView;
    private TextView tvPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_video_player);
        setTitle("本地视频播放");
        videoView = findViewById(R.id.videoViewLocal);
        tvPath = findViewById(R.id.tvLocalPlayerPath);

        String filePath = getIntent().getStringExtra(EXTRA_FILE_PATH);
        if (TextUtils.isEmpty(filePath) || !new File(filePath).exists()) {
            Toast.makeText(this, "本地文件不存在", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        tvPath.setText("文件: " + filePath);
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
        videoView.setVideoURI(Uri.fromFile(new File(filePath)));
        videoView.setOnPreparedListener(mp -> videoView.start());
    }

    @Override
    protected void onPause() {
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
        }
        super.onPause();
    }
}
