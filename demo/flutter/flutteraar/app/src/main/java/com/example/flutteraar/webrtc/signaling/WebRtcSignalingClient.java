package com.example.flutteraar.webrtc.signaling;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * WebRTC 信令客户端：负责与 SpringBoot WebSocket 信令服务收发消息。
 */
public class WebRtcSignalingClient {
    private static final String TAG = "WebRtcSignalingClient";

    public interface Listener {
        void onConnected();

        void onDisconnected(String reason);

        void onMessage(JSONObject message);

        void onError(String error);
    }

    private final String wsUrl;
    private final Listener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient okHttpClient;

    private volatile WebSocket webSocket;
    private volatile boolean manuallyClosed;

    public WebRtcSignalingClient(String signalingBaseUrl, String selfId, Listener listener) {
        this.wsUrl = buildWsUrl(signalingBaseUrl, selfId);
        this.listener = listener;
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .build();
    }

    public void connect() {
        if (manuallyClosed) {
            return;
        }
        Request request = new Request.Builder().url(wsUrl).build();
        webSocket = okHttpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.i(TAG, "onOpen, url=" + wsUrl);
                dispatchConnected();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    JSONObject message = new JSONObject(text);
                    dispatchMessage(message);
                } catch (JSONException e) {
                    Log.e(TAG, "onMessage parse failed, text=" + text, e);
                    dispatchError("信令消息解析失败: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
                Log.e(TAG, "onFailure", t);
                dispatchError("信令连接失败: " + t.getMessage());
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(code, reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.i(TAG, "onClosed, code=" + code + ", reason=" + reason);
                dispatchDisconnected(reason);
            }
        });
    }

    public boolean send(String type, @Nullable String to, @Nullable JSONObject payload, @Nullable String callId) {
        WebSocket socket = this.webSocket;
        if (socket == null) {
            return false;
        }
        try {
            JSONObject message = new JSONObject();
            message.put("type", type);
            if (!TextUtils.isEmpty(to)) {
                message.put("to", to);
            }
            if (!TextUtils.isEmpty(callId)) {
                message.put("callId", callId);
            }
            if (payload != null) {
                message.put("payload", payload);
            }
            return socket.send(message.toString());
        } catch (JSONException e) {
            Log.e(TAG, "send build json failed", e);
            dispatchError("发送信令失败: " + e.getMessage());
            return false;
        }
    }

    public void close() {
        manuallyClosed = true;
        WebSocket socket = this.webSocket;
        if (socket != null) {
            socket.close(1000, "client_close");
            this.webSocket = null;
        }
        okHttpClient.dispatcher().executorService().shutdown();
    }

    private String buildWsUrl(String signalingBaseUrl, String selfId) {
        if (TextUtils.isEmpty(signalingBaseUrl)) {
            throw new IllegalArgumentException("signalingBaseUrl is empty");
        }
        Uri baseUri = Uri.parse(signalingBaseUrl.trim());
        Uri.Builder builder = baseUri.buildUpon();
        builder.appendQueryParameter("uid", selfId);
        return builder.build().toString();
    }

    private void dispatchConnected() {
        mainHandler.post(listener::onConnected);
    }

    private void dispatchDisconnected(String reason) {
        mainHandler.post(() -> listener.onDisconnected(reason));
    }

    private void dispatchMessage(JSONObject message) {
        mainHandler.post(() -> listener.onMessage(message));
    }

    private void dispatchError(String error) {
        mainHandler.post(() -> listener.onError(error));
    }
}
