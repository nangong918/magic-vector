package com.example.flutteraar;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recyclerDemoList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<DemoItem> demoItems = Arrays.asList(
                new DemoItem("WiFi Demo", "获取 WiFi 信号强度/连接状态", WifiDemoActivity.class)
        );
        DemoListAdapter adapter = new DemoListAdapter(demoItems, item ->
                startActivity(new Intent(MainActivity.this, item.getTargetActivity())));
        recyclerView.setAdapter(adapter);
    }
}