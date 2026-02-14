package com.example.flutteraar;

import androidx.appcompat.app.AppCompatActivity;

public class DemoItem {
    private final String title;
    private final String description;
    private final Class<? extends AppCompatActivity> targetActivity;

    public DemoItem(String title, String description, Class<? extends AppCompatActivity> targetActivity) {
        this.title = title;
        this.description = description;
        this.targetActivity = targetActivity;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Class<? extends AppCompatActivity> getTargetActivity() {
        return targetActivity;
    }
}
