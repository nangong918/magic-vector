package com.example.flutteraar.ui.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.flutteraar.R;
import com.example.flutteraar.ui.fragment.VadSileroFragment;
import com.example.flutteraar.ui.fragment.VadWebRtcFragment;
import com.example.flutteraar.ui.fragment.VadYamnetFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class VADMainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vad_main);

        BottomNavigationView navView = findViewById(R.id.nav_view_vad);
        navView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_webrtc) {
                switchFragment(new VadWebRtcFragment());
                return true;
            } else if (item.getItemId() == R.id.navigation_silero) {
                switchFragment(new VadSileroFragment());
                return true;
            } else if (item.getItemId() == R.id.navigation_yamnet) {
                switchFragment(new VadYamnetFragment());
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            navView.setSelectedItemId(R.id.navigation_webrtc);
        }
    }

    private void switchFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_vad_activity_main, fragment)
                .commitAllowingStateLoss();
    }
}
