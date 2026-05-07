package com.example.flutteraar.media;

import android.content.Context;
import android.os.Environment;

import java.io.File;

public final class MediaDemoConfig {
    public static final String SERVER_BASE_URL = "http://192.168.1.3:48888";
    public static final long DEMO_USER_ID = 1L;
    public static final String DEMO_BUCKET_NAME = "global-oss";
    public static final String APP_MEDIA_ROOT = "magic-media-demo";

    private MediaDemoConfig() {
    }

    public static File getMediaRootDir(Context context) {
        File base = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (base == null) {
            base = context.getFilesDir();
        }
        File root = new File(base, APP_MEDIA_ROOT);
        if (!root.exists()) {
            root.mkdirs();
        }
        return root;
    }

    public static File getLocalVideoDir(Context context) {
        File dir = new File(getMediaRootDir(context), "local-video");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File getCloudDownloadDir(Context context) {
        File dir = new File(getMediaRootDir(context), "cloud-download");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File getHlsCacheDir(Context context) {
        File dir = new File(getMediaRootDir(context), "hls-cache");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
}
