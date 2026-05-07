package com.example.flutteraar.media.worker;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.flutteraar.media.MediaDemoConfig;
import com.example.flutteraar.media.SimpleHttpClient;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class HlsToMp4Worker extends Worker {
    public static final String TAG_CONVERT = "hls-to-mp4-convert";
    public static final String KEY_FILE_ID = "file_id";
    public static final String KEY_FILE_NAME = "file_name";
    public static final String KEY_BASE_URL = "base_url";
    public static final String KEY_DOWNLOADED = "downloaded";
    public static final String KEY_TOTAL = "total";
    public static final String KEY_STATUS = "status";
    public static final String KEY_OUTPUT_PATH = "output_path";

    public HlsToMp4Worker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static String buildUniqueWorkName(String fileId) {
        return "hls-to-mp4-" + fileId;
    }

    @NonNull
    @Override
    public Result doWork() {
        String fileId = getInputData().getString(KEY_FILE_ID);
        String baseUrl = getInputData().getString(KEY_BASE_URL);
        String fileName = getInputData().getString(KEY_FILE_NAME);
        if (TextUtils.isEmpty(baseUrl)) {
            baseUrl = MediaDemoConfig.SERVER_BASE_URL;
        }
        if (TextUtils.isEmpty(fileId)) {
            return Result.failure(new Data.Builder().putString(KEY_STATUS, "invalid-file-id").build());
        }
        try {
            updateProgress(0L, -1L, "request-convert", "");
            String downloadUrl = requestConvert(baseUrl, fileId);
            if (isStopped()) {
                return Result.failure(new Data.Builder().putString(KEY_STATUS, "cancelled").build());
            }
            File target = buildTargetFile(fileId, fileName);
            updateProgress(0L, -1L, "downloading", target.getAbsolutePath());
            SimpleHttpClient.downloadToFile(downloadUrl, target, (downloaded, total) -> {
                if (!isStopped()) {
                    updateProgress(downloaded, total, "downloading", target.getAbsolutePath());
                }
            });
            if (isStopped()) {
                return Result.failure(new Data.Builder().putString(KEY_STATUS, "cancelled").build());
            }
            long finalSize = target.length();
            updateProgress(finalSize, finalSize, "success", target.getAbsolutePath());
            return Result.success(new Data.Builder()
                    .putLong(KEY_DOWNLOADED, finalSize)
                    .putLong(KEY_TOTAL, finalSize)
                    .putString(KEY_STATUS, "success")
                    .putString(KEY_OUTPUT_PATH, target.getAbsolutePath())
                    .build());
        } catch (Exception e) {
            String message = e.getMessage() == null ? "convert-failed" : e.getMessage();
            if (isRetryable(message)) {
                return Result.retry();
            }
            return Result.failure(new Data.Builder()
                    .putString(KEY_STATUS, message)
                    .build());
        }
    }

    private String requestConvert(String baseUrl, String fileId) throws Exception {
        Map<String, String> form = new HashMap<>();
        form.put("fileId", fileId);
        JSONObject response = SimpleHttpClient.postForm(baseUrl + "/video/cloud/hls/to-mp4", form);
        JSONObject data = requireData(response);
        String downloadUrl = data.optString("downloadUrl", "");
        if (TextUtils.isEmpty(downloadUrl)) {
            throw new IllegalStateException("download-url-empty");
        }
        return downloadUrl;
    }

    private JSONObject requireData(JSONObject response) throws Exception {
        String code = response.optString("code", "400");
        if (!"200".equals(code)) {
            throw new IllegalStateException(response.optString("message", "request-failed"));
        }
        JSONObject data = response.optJSONObject("data");
        if (data == null) {
            throw new IllegalStateException("response-data-empty");
        }
        return data;
    }

    private File buildTargetFile(String fileId, String fileName) {
        String safeName = fileName == null ? "" : fileName.trim();
        if (safeName.isEmpty()) {
            safeName = "file_" + fileId;
        }
        safeName = safeName.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (safeName.toLowerCase().endsWith(".mp4")) {
            safeName = safeName.substring(0, safeName.length() - 4);
        }
        File dir = MediaDemoConfig.getHlsMp4Dir(getApplicationContext());
        File target = new File(dir, safeName + "_hls.mp4");
        if (!target.exists()) {
            return target;
        }
        int index = 1;
        while (target.exists()) {
            target = new File(dir, safeName + "_hls_" + index + ".mp4");
            index++;
        }
        return target;
    }

    private void updateProgress(long downloaded, long total, String status, String outputPath) {
        Data.Builder builder = new Data.Builder()
                .putLong(KEY_DOWNLOADED, downloaded)
                .putLong(KEY_TOTAL, total)
                .putString(KEY_STATUS, status);
        if (!TextUtils.isEmpty(outputPath)) {
            builder.putString(KEY_OUTPUT_PATH, outputPath);
        }
        setProgressAsync(builder.build());
    }

    private boolean isRetryable(String message) {
        String lower = message == null ? "" : message.toLowerCase();
        return lower.contains("timeout")
                || lower.contains("failed to connect")
                || lower.contains("connection reset")
                || lower.contains("http 5");
    }
}
