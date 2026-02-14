package com.example.flutteraar;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.demo.aarlib.voicewakeup.VoiceWakeUpBridge;
import com.example.flutteraar.domain.entity.DemoItem;
import com.example.flutteraar.ui.activity.WifiDemoActivity;
import com.example.flutteraar.ui.activity.BatteryDemoActivity;
import com.example.flutteraar.ui.activity.IvwDemoActivity;
import com.example.flutteraar.ui.adapter.DemoListAdapter;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        VoiceWakeUpBridge.setDefaultConfig(new VoiceWakeUpBridge.Config(
                getString(R.string.appId),
                getString(R.string.apiKey),
                getString(R.string.apiSecret),
                "",
                getString(R.string.ivwAbilityId)
        ));

        RecyclerView recyclerView = findViewById(R.id.recyclerDemoList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<DemoItem> demoItems = Arrays.asList(
                new DemoItem("WiFi Demo", "获取 WiFi 信号强度/连接状态", WifiDemoActivity.class),
                new DemoItem("Battery Demo", "获取 电池电量", BatteryDemoActivity.class),
                new DemoItem("离线唤醒 Demo", "初始化SDK / 录音唤醒 / 文件唤醒", IvwDemoActivity.class)
        );
        DemoListAdapter adapter = new DemoListAdapter(demoItems, item ->
                startActivity(new Intent(MainActivity.this, item.getTargetActivity())));
        recyclerView.setAdapter(adapter);
    }
}