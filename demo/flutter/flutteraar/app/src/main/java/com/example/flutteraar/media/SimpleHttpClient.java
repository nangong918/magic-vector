package com.example.flutteraar.media;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class SimpleHttpClient {
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 60_000;
    private static final String UTF_8 = "UTF-8";

    private SimpleHttpClient() {
    }

    public static JSONObject getJson(String url) throws Exception {
        HttpURLConnection conn = createConnection(url, "GET");
        return parseJsonResponse(conn);
    }

    public static String getText(String url) throws Exception {
        HttpURLConnection conn = createConnection(url, "GET");
        int code = conn.getResponseCode();
        String text = readString(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
        if (code < 200 || code >= 300) {
            throw new IOException("http " + code + ": " + text);
        }
        return text;
    }

    public static JSONObject postForm(String url, Map<String, String> form) throws Exception {
        HttpURLConnection conn = createConnection(url, "POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        conn.setDoOutput(true);
        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> entry : form.entrySet()) {
            if (body.length() > 0) {
                body.append("&");
            }
            body.append(URLEncoder.encode(entry.getKey(), UTF_8));
            body.append("=");
            body.append(URLEncoder.encode(entry.getValue(), UTF_8));
        }
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream out = conn.getOutputStream()) {
            out.write(bytes);
            out.flush();
        }
        return parseJsonResponse(conn);
    }

    public static JSONObject postMultipart(
            String url,
            Map<String, String> form,
            String fileFieldName,
            String fileName,
            String contentType,
            byte[] fileBytes
    ) throws Exception {
        String boundary = "----MagicVectorBoundary" + System.currentTimeMillis();
        HttpURLConnection conn = createConnection(url, "POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        try (BufferedOutputStream out = new BufferedOutputStream(conn.getOutputStream())) {
            for (Map.Entry<String, String> entry : form.entrySet()) {
                writeFormField(out, boundary, entry.getKey(), entry.getValue());
            }
            out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Disposition: form-data; name=\"" + fileFieldName + "\"; filename=\"" + fileName + "\"\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(fileBytes);
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
        return parseJsonResponse(conn);
    }

    public static JSONObject postEmpty(String url) throws Exception {
        HttpURLConnection conn = createConnection(url, "POST");
        conn.setDoOutput(true);
        conn.getOutputStream().close();
        return parseJsonResponse(conn);
    }

    public static void downloadToFile(String url, File targetFile, ProgressCallback callback) throws Exception {
        HttpURLConnection conn = createConnection(url, "GET");
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IOException("http " + code + ": " + readString(conn.getErrorStream()));
        }
        long total = conn.getContentLengthLong();
        if (targetFile.getParentFile() != null && !targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }
        try (InputStream in = new BufferedInputStream(conn.getInputStream());
             FileOutputStream out = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[32 * 1024];
            long downloaded = 0L;
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
                downloaded += len;
                if (callback != null) {
                    callback.onProgress(downloaded, total);
                }
            }
            out.flush();
        }
    }

    private static HttpURLConnection createConnection(String url, String method) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setUseCaches(false);
        return conn;
    }

    private static JSONObject parseJsonResponse(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        String text = readString(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
        if (code < 200 || code >= 300) {
            throw new IOException("http " + code + ": " + text);
        }
        return new JSONObject(text);
    }

    private static String readString(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        try (InputStream in = inputStream;
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8 * 1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            return out.toString(UTF_8);
        }
    }

    private static void writeFormField(BufferedOutputStream out, String boundary, String key, String value) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + key + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(value.getBytes(StandardCharsets.UTF_8));
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    public interface ProgressCallback {
        void onProgress(long downloaded, long total);
    }
}
