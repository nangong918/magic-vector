package com.example.flutteraar.ui.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.demo.aarlib.WifiNativeBridge;
import com.example.flutteraar.R;

public class WifiDemoActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_demo);
        setTitle("WiFi Demo");

        TextView tvSignal = findViewById(R.id.tvSignal);
        TextView tvConnected = findViewById(R.id.tvConnected);
        Button btnRefresh = findViewById(R.id.btnRefreshWifi);

        btnRefresh.setOnClickListener(v -> refreshWifiInfo(tvSignal, tvConnected));
        refreshWifiInfo(tvSignal, tvConnected);
    }

    @SuppressLint("SetTextI18n")
    private void refreshWifiInfo(TextView tvSignal, TextView tvConnected) {
        int signal = WifiNativeBridge.getWifiSignalStrength(this);
        boolean connected = WifiNativeBridge.isWifiConnected(this);

        if (signal == WifiNativeBridge.WIFI_SIGNAL_UNAVAILABLE) {
            tvSignal.setText("信号强度: 不可用（未开启或未连接）");
        } else {
            tvSignal.setText("信号强度: " + signal + " dBm");
        }
        tvConnected.setText("WiFi连接状态: " + (connected ? "已连接" : "未连接"));
    }
}
