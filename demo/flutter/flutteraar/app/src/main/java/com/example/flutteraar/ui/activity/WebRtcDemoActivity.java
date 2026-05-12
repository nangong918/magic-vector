package com.example.flutteraar.ui.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Build;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.flutteraar.R;
import com.example.flutteraar.webrtc.signaling.WebRtcSignalTypes;
import com.example.flutteraar.webrtc.signaling.WebRtcSignalingService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * WebRTC 演示入口页：
 * - 首次进入必须绑定本机 ID；
 * - 可输入对方 ID 发起呼叫；
 * - 可接收来电并决定接听/拒绝。
 */
public class WebRtcDemoActivity extends AppCompatActivity {
    private static final String DEFAULT_SIGNAL_URL = "ws://192.168.1.3/ws/webrtc";
    private static final String PREFS_WEBRTC = "prefs_webrtc";
    private static final String KEY_SELF_ID = "self_id";
    private static final String KEY_SIGNAL_URL = "signal_url";

    private TextView tvSelfId;
    private TextView tvSignalStatus;
    private EditText editSignalUrl;
    private EditText editPeerId;
    private Button btnCall;
    private Button btnRebind;
    private Button btnReconnect;

    private String selfId;
    private String pendingCallId;
    private String pendingPeerId;
    private String pendingIncomingFrom;
    private String pendingIncomingCallId;
    private boolean signalingConnected;
    private boolean receiverRegistered;
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            });

    private final BroadcastReceiver signalingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!WebRtcSignalingService.ACTION_SIGNALING_EVENT.equals(intent.getAction())) {
                return;
            }
            String kind = intent.getStringExtra(WebRtcSignalingService.EXTRA_EVENT_KIND);
            String message = intent.getStringExtra(WebRtcSignalingService.EXTRA_EVENT_MESSAGE);
            String json = intent.getStringExtra(WebRtcSignalingService.EXTRA_EVENT_JSON);
            if (WebRtcSignalingService.EVENT_CONNECTED.equals(kind)) {
                signalingConnected = true;
                updateSignalStatus("已连接，等待呼叫");
                updateButtons();
                return;
            }
            if (WebRtcSignalingService.EVENT_DISCONNECTED.equals(kind)) {
                signalingConnected = false;
                updateSignalStatus("连接断开: " + (TextUtils.isEmpty(message) ? "unknown" : message));
                updateButtons();
                return;
            }
            if (WebRtcSignalingService.EVENT_ERROR.equals(kind)) {
                signalingConnected = false;
                updateSignalStatus(TextUtils.isEmpty(message) ? "信令异常" : message);
                Toast.makeText(WebRtcDemoActivity.this, updateNullable(message, "信令异常"), Toast.LENGTH_SHORT).show();
                updateButtons();
                return;
            }
            if (WebRtcSignalingService.EVENT_MESSAGE.equals(kind) && !TextUtils.isEmpty(json)) {
                try {
                    handleSignalMessage(new JSONObject(json));
                } catch (JSONException e) {
                    updateSignalStatus("信令消息解析失败");
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webrtc_demo);
        setTitle("WebRTC Demo");
        bindViews();
        initListeners();
        requestNotificationPermissionIfNeeded();
        loadBindingFromPrefs();
        handleIncomingIntent(getIntent());
        if (TextUtils.isEmpty(selfId)) {
            showBindDialog(false);
        }
    }

    private void bindViews() {
        tvSelfId = findViewById(R.id.tvWebRtcSelfId);
        tvSignalStatus = findViewById(R.id.tvWebRtcSignalStatus);
        editSignalUrl = findViewById(R.id.editWebRtcSignalUrl);
        editPeerId = findViewById(R.id.editWebRtcPeerId);
        btnCall = findViewById(R.id.btnWebRtcCall);
        btnRebind = findViewById(R.id.btnWebRtcRebind);
        btnReconnect = findViewById(R.id.btnWebRtcReconnect);

        editSignalUrl.setText(DEFAULT_SIGNAL_URL);
        updateSelfIdText();
        updateSignalStatus("未连接");
        updateButtons();
    }

    private void initListeners() {
        btnCall.setOnClickListener(v -> tryCallPeer());
        btnRebind.setOnClickListener(v -> showBindDialog(true));
        btnReconnect.setOnClickListener(v -> connectSignaling());
    }

    private void showBindDialog(boolean allowCancel) {
        EditText input = new EditText(this);
        input.setHint("请输入本机ID（如 deviceA）");
        input.setSingleLine();
        if (!TextUtils.isEmpty(selfId)) {
            input.setText(selfId);
            input.setSelection(selfId.length());
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("绑定本机ID")
                .setView(input)
                .setCancelable(allowCancel)
                .setNegativeButton("取消", (dialog, which) -> {
                    if (!allowCancel && TextUtils.isEmpty(selfId)) {
                        finish();
                    }
                })
                .setPositiveButton("绑定", null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dlg -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String inputId = input.getText().toString().trim();
            if (TextUtils.isEmpty(inputId)) {
                input.setError("ID 不能为空");
                return;
            }
            selfId = inputId;
            pendingCallId = null;
            pendingPeerId = null;
            updateSelfIdText();
            saveBindingToPrefs();
            dialog.dismiss();
            connectSignaling();
        }));
        dialog.show();
    }

    private void tryCallPeer() {
        if (TextUtils.isEmpty(selfId)) {
            Toast.makeText(this, "请先绑定本机 ID", Toast.LENGTH_SHORT).show();
            showBindDialog(false);
            return;
        }
        String peerId = editPeerId.getText().toString().trim();
        if (TextUtils.isEmpty(peerId)) {
            editPeerId.setError("请输入对方ID");
            return;
        }
        if (peerId.equals(selfId)) {
            editPeerId.setError("不能呼叫自己");
            return;
        }
        if (!signalingConnected) {
            Toast.makeText(this, "信令未连接，请重连", Toast.LENGTH_SHORT).show();
            return;
        }

        String callId = UUID.randomUUID().toString();
        sendSignal(WebRtcSignalTypes.CALL_INVITE, peerId, callId, null);
        pendingCallId = callId;
        pendingPeerId = peerId;
        updateSignalStatus("呼叫中，等待对方接听...");
    }

    private void connectSignaling() {
        if (TextUtils.isEmpty(selfId)) {
            return;
        }
        String signalBaseUrl = editSignalUrl.getText().toString().trim();
        if (TextUtils.isEmpty(signalBaseUrl)) {
            editSignalUrl.setError("请输入信令地址");
            return;
        }
        saveBindingToPrefs();
        Intent serviceIntent = new Intent(this, WebRtcSignalingService.class);
        serviceIntent.setAction(WebRtcSignalingService.ACTION_START_SIGNALING);
        serviceIntent.putExtra(WebRtcSignalingService.EXTRA_SELF_ID, selfId);
        serviceIntent.putExtra(WebRtcSignalingService.EXTRA_SIGNAL_URL, signalBaseUrl);
        ContextCompat.startForegroundService(this, serviceIntent);
        signalingConnected = false;
        updateSignalStatus("连接中...");
        updateButtons();
    }

    private void handleSignalMessage(JSONObject message) {
        String type = message.optString("type");
        if (WebRtcSignalTypes.BIND_OK.equals(type)) {
            updateSignalStatus("绑定成功，可呼叫");
            return;
        }
        if (WebRtcSignalTypes.ERROR.equals(type)) {
            JSONObject payload = message.optJSONObject("payload");
            String error = payload == null ? "信令错误" : payload.optString("message", "信令错误");
            updateSignalStatus(error);
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            return;
        }
        if (WebRtcSignalTypes.CALL_INVITE.equals(type)) {
            showIncomingCallDialog(message.optString("from"), message.optString("callId"));
            return;
        }
        if (WebRtcSignalTypes.CALL_ACCEPT.equals(type)) {
            String from = message.optString("from");
            String callId = message.optString("callId");
            if (!TextUtils.isEmpty(pendingCallId)
                    && pendingCallId.equals(callId)
                    && TextUtils.equals(pendingPeerId, from)) {
                launchCallActivity(from, callId, true);
            }
            return;
        }
        if (WebRtcSignalTypes.CALL_REJECT.equals(type)) {
            String from = message.optString("from");
            updateSignalStatus("对方拒绝了呼叫: " + from);
            pendingCallId = null;
            pendingPeerId = null;
            return;
        }
        if (WebRtcSignalTypes.HANGUP.equals(type)) {
            updateSignalStatus("对方已挂断");
        }
    }

    private void showIncomingCallDialog(String from, String callId) {
        if (TextUtils.isEmpty(from) || TextUtils.isEmpty(callId)) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("收到视频通话")
                .setMessage("来自 " + from + " 的来电，是否接听？")
                .setNegativeButton("拒绝", (dialog, which) -> {
                    sendSignal(WebRtcSignalTypes.CALL_REJECT, from, callId, null);
                    updateSignalStatus("已拒绝 " + from + " 的来电");
                })
                .setPositiveButton("接听", (dialog, which) -> {
                    sendSignal(WebRtcSignalTypes.CALL_ACCEPT, from, callId, null);
                    launchCallActivity(from, callId, false);
                })
                .setCancelable(false)
                .show();
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        String from = intent.getStringExtra(WebRtcSignalingService.EXTRA_INCOMING_FROM);
        String callId = intent.getStringExtra(WebRtcSignalingService.EXTRA_INCOMING_CALL_ID);
        if (!TextUtils.isEmpty(from) && !TextUtils.isEmpty(callId)) {
            pendingIncomingFrom = from;
            pendingIncomingCallId = callId;
        }
    }

    private void consumePendingIncomingIfNeeded() {
        if (TextUtils.isEmpty(pendingIncomingFrom) || TextUtils.isEmpty(pendingIncomingCallId)) {
            return;
        }
        String from = pendingIncomingFrom;
        String callId = pendingIncomingCallId;
        pendingIncomingFrom = null;
        pendingIncomingCallId = null;
        showIncomingCallDialog(from, callId);
    }

    private void launchCallActivity(String peerId, String callId, boolean caller) {
        if (TextUtils.isEmpty(peerId) || TextUtils.isEmpty(callId) || TextUtils.isEmpty(selfId)) {
            return;
        }
        String signalUrl = editSignalUrl.getText().toString().trim();
        Intent intent = new Intent(this, WebRtcCallActivity.class);
        intent.putExtra(WebRtcCallActivity.EXTRA_SELF_ID, selfId);
        intent.putExtra(WebRtcCallActivity.EXTRA_PEER_ID, peerId);
        intent.putExtra(WebRtcCallActivity.EXTRA_CALL_ID, callId);
        intent.putExtra(WebRtcCallActivity.EXTRA_SIGNAL_URL, signalUrl);
        intent.putExtra(WebRtcCallActivity.EXTRA_IS_CALLER, caller);
        startActivity(intent);
    }

    private void sendSignal(String type, String to, String callId, JSONObject payload) {
        Intent intent = new Intent(this, WebRtcSignalingService.class);
        intent.setAction(WebRtcSignalingService.ACTION_SEND_SIGNAL);
        intent.putExtra(WebRtcSignalingService.EXTRA_SIGNAL_TYPE, type);
        intent.putExtra(WebRtcSignalingService.EXTRA_TO, to);
        intent.putExtra(WebRtcSignalingService.EXTRA_CALL_ID, callId);
        if (payload != null) {
            intent.putExtra(WebRtcSignalingService.EXTRA_PAYLOAD_JSON, payload.toString());
        }
        startService(intent);
    }

    private String updateNullable(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private void loadBindingFromPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_WEBRTC, MODE_PRIVATE);
        String cachedSelfId = prefs.getString(KEY_SELF_ID, null);
        String cachedSignalUrl = prefs.getString(KEY_SIGNAL_URL, DEFAULT_SIGNAL_URL);
        if (!TextUtils.isEmpty(cachedSelfId)) {
            selfId = cachedSelfId;
            updateSelfIdText();
        }
        if (!TextUtils.isEmpty(cachedSignalUrl)) {
            editSignalUrl.setText(cachedSignalUrl);
        }
    }

    private void saveBindingToPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_WEBRTC, MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_SELF_ID, selfId)
                .putString(KEY_SIGNAL_URL, editSignalUrl.getText().toString().trim())
                .apply();
    }

    private void registerSignalingReceiver() {
        if (receiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter(WebRtcSignalingService.ACTION_SIGNALING_EVENT);
        ContextCompat.registerReceiver(this, signalingReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        receiverRegistered = true;
    }

    private void unregisterSignalingReceiver() {
        if (!receiverRegistered) {
            return;
        }
        unregisterReceiver(signalingReceiver);
        receiverRegistered = false;
    }

    private void updateSelfIdText() {
        tvSelfId.setText("本机ID: " + (TextUtils.isEmpty(selfId) ? "未绑定" : selfId));
    }

    private void updateSignalStatus(String message) {
        tvSignalStatus.setText("信令状态: " + message);
    }

    private void updateButtons() {
        boolean ready = !TextUtils.isEmpty(selfId) && signalingConnected;
        btnCall.setEnabled(ready);
        btnReconnect.setEnabled(!TextUtils.isEmpty(selfId));
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerSignalingReceiver();
        if (!TextUtils.isEmpty(selfId)) {
            connectSignaling();
        }
        consumePendingIncomingIfNeeded();
    }

    @Override
    protected void onStop() {
        unregisterSignalingReceiver();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
        consumePendingIncomingIfNeeded();
    }
}
