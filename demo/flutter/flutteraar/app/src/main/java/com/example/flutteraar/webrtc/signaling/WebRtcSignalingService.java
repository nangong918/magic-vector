package com.example.flutteraar.webrtc.signaling;

import android.app.PendingIntent;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.flutteraar.R;
import com.example.flutteraar.ui.activity.WebRtcDemoActivity;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 常驻前台服务：维持 WebRTC 信令 WebSocket 连接。
 */
public class WebRtcSignalingService extends Service {
    private static final int NOTIFICATION_ID = 20131;
    private static final int INCOMING_CALL_NOTIFICATION_ID = 20132;
    private static final String CHANNEL_ID = "webrtc_signaling_channel";

    public static final String ACTION_START_SIGNALING = "com.example.flutteraar.webrtc.action.START_SIGNALING";
    public static final String ACTION_SEND_SIGNAL = "com.example.flutteraar.webrtc.action.SEND_SIGNAL";
    public static final String ACTION_STOP_SIGNALING = "com.example.flutteraar.webrtc.action.STOP_SIGNALING";
    public static final String ACTION_SIGNALING_EVENT = "com.example.flutteraar.webrtc.action.SIGNALING_EVENT";

    public static final String EXTRA_SELF_ID = "extra_self_id";
    public static final String EXTRA_SIGNAL_URL = "extra_signal_url";
    public static final String EXTRA_SIGNAL_TYPE = "extra_signal_type";
    public static final String EXTRA_TO = "extra_to";
    public static final String EXTRA_CALL_ID = "extra_call_id";
    public static final String EXTRA_PAYLOAD_JSON = "extra_payload_json";

    public static final String EXTRA_EVENT_KIND = "extra_event_kind";
    public static final String EXTRA_EVENT_MESSAGE = "extra_event_message";
    public static final String EXTRA_EVENT_JSON = "extra_event_json";
    public static final String EXTRA_INCOMING_FROM = "extra_incoming_from";
    public static final String EXTRA_INCOMING_CALL_ID = "extra_incoming_call_id";

    public static final String EVENT_CONNECTED = "connected";
    public static final String EVENT_DISCONNECTED = "disconnected";
    public static final String EVENT_ERROR = "error";
    public static final String EVENT_MESSAGE = "message";

    private WebRtcSignalingClient signalingClient;
    private String currentSelfId;
    private String currentSignalUrl;
    private boolean connected;
    private boolean foregroundStarted;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundInternal("信令连接准备中");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_STICKY;
        }
        String action = intent.getAction();
        if (ACTION_START_SIGNALING.equals(action)) {
            String selfId = intent.getStringExtra(EXTRA_SELF_ID);
            String signalUrl = intent.getStringExtra(EXTRA_SIGNAL_URL);
            ensureConnected(selfId, signalUrl);
            return START_STICKY;
        }
        if (ACTION_SEND_SIGNAL.equals(action)) {
            handleSendSignal(intent);
            return START_STICKY;
        }
        if (ACTION_STOP_SIGNALING.equals(action)) {
            closeSignaling();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void ensureConnected(String selfId, String signalUrl) {
        if (TextUtils.isEmpty(selfId) || TextUtils.isEmpty(signalUrl)) {
            broadcastEvent(EVENT_ERROR, "selfId 或 signalUrl 为空", null);
            return;
        }
        boolean sameConfig = TextUtils.equals(selfId, currentSelfId) && TextUtils.equals(signalUrl, currentSignalUrl);
        if (sameConfig && signalingClient != null) {
            if (connected) {
                broadcastEvent(EVENT_CONNECTED, "信令已连接", null);
            }
            return;
        }

        closeSignaling();
        currentSelfId = selfId;
        currentSignalUrl = signalUrl;
        updateNotification("信令连接中: " + selfId);
        signalingClient = new WebRtcSignalingClient(signalUrl, selfId, new WebRtcSignalingClient.Listener() {
            @Override
            public void onConnected() {
                connected = true;
                updateNotification("在线: " + currentSelfId);
                broadcastEvent(EVENT_CONNECTED, "信令已连接", null);
            }

            @Override
            public void onDisconnected(String reason) {
                connected = false;
                updateNotification("连接已断开");
                broadcastEvent(EVENT_DISCONNECTED, reason, null);
            }

            @Override
            public void onMessage(JSONObject message) {
                String type = message.optString("type");
                if (WebRtcSignalTypes.CALL_INVITE.equals(type)) {
                    showIncomingCallNotification(
                            message.optString("from"),
                            message.optString("callId")
                    );
                }
                broadcastEvent(EVENT_MESSAGE, null, message);
            }

            @Override
            public void onError(String error) {
                connected = false;
                updateNotification("连接异常");
                broadcastEvent(EVENT_ERROR, error, null);
            }
        });
        signalingClient.connect();
    }

    private void handleSendSignal(Intent intent) {
        if (signalingClient == null) {
            broadcastEvent(EVENT_ERROR, "信令未初始化", null);
            return;
        }
        String type = intent.getStringExtra(EXTRA_SIGNAL_TYPE);
        String to = intent.getStringExtra(EXTRA_TO);
        String callId = intent.getStringExtra(EXTRA_CALL_ID);
        String payloadJson = intent.getStringExtra(EXTRA_PAYLOAD_JSON);
        JSONObject payload = null;
        if (!TextUtils.isEmpty(payloadJson)) {
            try {
                payload = new JSONObject(payloadJson);
            } catch (JSONException e) {
                broadcastEvent(EVENT_ERROR, "payload 不是合法 JSON", null);
                return;
            }
        }
        boolean sent = signalingClient.send(type, to, payload, callId);
        if (!sent) {
            broadcastEvent(EVENT_ERROR, "信令发送失败: " + type, null);
        }
    }

    private void closeSignaling() {
        connected = false;
        if (signalingClient != null) {
            signalingClient.close();
            signalingClient = null;
        }
    }

    private void broadcastEvent(String kind, @Nullable String message, @Nullable JSONObject json) {
        Intent event = new Intent(ACTION_SIGNALING_EVENT);
        event.setPackage(getPackageName());
        event.putExtra(EXTRA_EVENT_KIND, kind);
        if (!TextUtils.isEmpty(message)) {
            event.putExtra(EXTRA_EVENT_MESSAGE, message);
        }
        if (json != null) {
            event.putExtra(EXTRA_EVENT_JSON, json.toString());
        }
        sendBroadcast(event);
    }

    private void startForegroundInternal(String content) {
        if (foregroundStarted) {
            return;
        }
        createNotificationChannelIfNeeded();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("WebRTC 信令服务")
                .setContentText(content)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
        startForeground(NOTIFICATION_ID, notification);
        foregroundStarted = true;
    }

    private void updateNotification(String content) {
        createNotificationChannelIfNeeded();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("WebRTC 信令服务")
                .setContentText(content)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    private void createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "WebRTC 信令",
                NotificationManager.IMPORTANCE_LOW
        );
        manager.createNotificationChannel(channel);
    }

    private void showIncomingCallNotification(String from, String callId) {
        if (TextUtils.isEmpty(from) || TextUtils.isEmpty(callId)) {
            return;
        }
        Intent openIntent = new Intent(this, WebRtcDemoActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        openIntent.putExtra(EXTRA_INCOMING_FROM, from);
        openIntent.putExtra(EXTRA_INCOMING_CALL_ID, callId);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                INCOMING_CALL_NOTIFICATION_ID,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("收到视频来电")
                .setContentText("来自 " + from + "，点击接听或拒绝")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .build();
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(INCOMING_CALL_NOTIFICATION_ID, notification);
        }
    }

    @Override
    public void onDestroy() {
        closeSignaling();
        super.onDestroy();
    }
}
