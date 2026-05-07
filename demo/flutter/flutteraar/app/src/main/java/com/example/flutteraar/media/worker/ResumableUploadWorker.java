package com.example.flutteraar.media.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.flutteraar.media.MediaDemoConfig;
import com.example.flutteraar.media.SimpleHttpClient;

import org.json.JSONObject;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

public class ResumableUploadWorker extends Worker {
    public static final String TAG_UPLOAD = "resumable-video-upload";
    public static final String KEY_FILE_PATH = "file_path";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_BUCKET_NAME = "bucket_name";
    public static final String KEY_BASE_URL = "base_url";
    public static final String KEY_UPLOADED = "uploaded";
    public static final String KEY_TOTAL = "total";
    public static final String KEY_STATUS = "status";

    private static final int CHUNK_SIZE = 1024 * 1024;

    public ResumableUploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String filePath = getInputData().getString(KEY_FILE_PATH);
        long userId = getInputData().getLong(KEY_USER_ID, MediaDemoConfig.DEMO_USER_ID);
        String bucketName = getInputData().getString(KEY_BUCKET_NAME);
        String baseUrl = getInputData().getString(KEY_BASE_URL);
        if (bucketName == null || bucketName.trim().isEmpty()) {
            bucketName = MediaDemoConfig.DEMO_BUCKET_NAME;
        }
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            baseUrl = MediaDemoConfig.SERVER_BASE_URL;
        }
        if (filePath == null || filePath.trim().isEmpty()) {
            return Result.failure(new Data.Builder().putString(KEY_STATUS, "invalid-file-path").build());
        }
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            return Result.failure(new Data.Builder().putString(KEY_STATUS, "file-not-found").build());
        }
        long totalBytes = file.length();
        try {
            JSONObject initData = initUpload(baseUrl, userId, bucketName, file.getName(), totalBytes);
            String sessionId = initData.optString("sessionId", "");
            long uploadedBytes = initData.optLong("uploadedBytes", 0L);
            if (sessionId.isEmpty()) {
                return Result.failure(new Data.Builder().putString(KEY_STATUS, "session-init-failed").build());
            }
            if (uploadedBytes > totalBytes) {
                uploadedBytes = 0L;
            }
            updateProgress(uploadedBytes, totalBytes, "running");
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                raf.seek(uploadedBytes);
                byte[] buffer = new byte[CHUNK_SIZE];
                while (uploadedBytes < totalBytes) {
                    if (isStopped()) {
                        updateProgress(uploadedBytes, totalBytes, "paused");
                        return Result.failure(new Data.Builder()
                                .putLong(KEY_UPLOADED, uploadedBytes)
                                .putLong(KEY_TOTAL, totalBytes)
                                .putString(KEY_STATUS, "paused")
                                .build());
                    }
                    int maxRead = (int) Math.min(buffer.length, totalBytes - uploadedBytes);
                    int len = raf.read(buffer, 0, maxRead);
                    if (len <= 0) {
                        break;
                    }
                    byte[] chunk = new byte[len];
                    System.arraycopy(buffer, 0, chunk, 0, len);
                    JSONObject chunkData = uploadChunk(baseUrl, sessionId, uploadedBytes, file.getName(), chunk);
                    uploadedBytes = chunkData.optLong("uploadedBytes", uploadedBytes + len);
                    updateProgress(uploadedBytes, totalBytes, "running");
                }
            }
            JSONObject completeData = completeUpload(baseUrl, sessionId);
            updateProgress(totalBytes, totalBytes, "success");
            return Result.success(new Data.Builder()
                    .putLong(KEY_UPLOADED, totalBytes)
                    .putLong(KEY_TOTAL, totalBytes)
                    .putString(KEY_STATUS, completeData.optString("message", "success"))
                    .build());
        } catch (Exception e) {
            String message = e.getMessage() == null ? "upload-failed" : e.getMessage();
            if (isRetryable(message)) {
                return Result.retry();
            }
            return Result.failure(new Data.Builder().putString(KEY_STATUS, message).build());
        }
    }

    private JSONObject initUpload(String baseUrl, long userId, String bucketName, String fileName, long fileSize) throws Exception {
        Map<String, String> form = new HashMap<>();
        form.put("userId", String.valueOf(userId));
        form.put("bucketName", bucketName);
        form.put("fileName", fileName);
        form.put("fileSize", String.valueOf(fileSize));
        JSONObject response = SimpleHttpClient.postForm(baseUrl + "/video/upload/init", form);
        return requireData(response);
    }

    private JSONObject uploadChunk(String baseUrl, String sessionId, long offset, String fileName, byte[] bytes) throws Exception {
        Map<String, String> form = new HashMap<>();
        form.put("sessionId", sessionId);
        form.put("offset", String.valueOf(offset));
        JSONObject response = SimpleHttpClient.postMultipart(
                baseUrl + "/video/upload/chunk",
                form,
                "chunkFile",
                fileName,
                "application/octet-stream",
                bytes
        );
        return requireData(response);
    }

    private JSONObject completeUpload(String baseUrl, String sessionId) throws Exception {
        Map<String, String> form = new HashMap<>();
        form.put("sessionId", sessionId);
        JSONObject response = SimpleHttpClient.postForm(baseUrl + "/video/upload/complete", form);
        return requireData(response);
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

    private void updateProgress(long uploaded, long total, String status) {
        setProgressAsync(new Data.Builder()
                .putLong(KEY_UPLOADED, uploaded)
                .putLong(KEY_TOTAL, total)
                .putString(KEY_STATUS, status)
                .build());
    }

    private boolean isRetryable(String message) {
        String lower = message == null ? "" : message.toLowerCase();
        return lower.contains("timeout")
                || lower.contains("failed to connect")
                || lower.contains("connection reset")
                || lower.contains("http 5");
    }
}
