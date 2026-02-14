package com.example.flutteraar.ui.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.demo.aarlib.BatteryNativeBridge;
import com.example.flutteraar.R;

public class BatteryDemoActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battery_demo);
        setTitle("Battery Demo");

        TextView tvBattery = findViewById(R.id.tvBattery);
        Button btnRefresh = findViewById(R.id.btnRefreshBattery);

        btnRefresh.setOnClickListener(v -> refreshBatteryInfo(tvBattery));
        refreshBatteryInfo(tvBattery);
    }

    private void refreshBatteryInfo(TextView tvBattery) {
        int batteryLevel = BatteryNativeBridge.getBatteryLevel(this);

        if (batteryLevel == -1) {
            tvBattery.setText("电池电量: 不可用");
        } else {
            tvBattery.setText("电池电量: " + batteryLevel + "%");
        }
    }
}
