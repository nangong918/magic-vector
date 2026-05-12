package com.example.flutteraar.ui.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.flutteraar.R;
import com.example.flutteraar.webrtc.signaling.WebRtcSignalTypes;
import com.example.flutteraar.webrtc.signaling.WebRtcSignalingService;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

/**
 * WebRTC 通话页：
 * - 上方显示本地摄像头；
 * - 下方显示对端画面；
 * - 提供静音、开关摄像头、挂断按钮。
 */
public class WebRtcCallActivity extends AppCompatActivity {
    private static final String TAG = "WebRtcCallActivity";
    private static final int VIDEO_WIDTH = 640;
    private static final int VIDEO_HEIGHT = 480;
    private static final int VIDEO_FPS = 30;
    private static final String DEFAULT_TURN_USER = "webrtc";
    private static final String DEFAULT_TURN_PASSWORD = "webrtc123";

    public static final String EXTRA_SELF_ID = "extra_self_id";
    public static final String EXTRA_PEER_ID = "extra_peer_id";
    public static final String EXTRA_CALL_ID = "extra_call_id";
    public static final String EXTRA_SIGNAL_URL = "extra_signal_url";
    public static final String EXTRA_IS_CALLER = "extra_is_caller";

    private SurfaceViewRenderer localRenderer;
    private SurfaceViewRenderer remoteRenderer;
    private TextView tvStatus;
    private ToggleButton btnMute;
    private ToggleButton btnCamera;
    private Button btnHangup;

    private String selfId;
    private String peerId;
    private String callId;
    private String signalUrl;
    private boolean caller;

    private EglBase eglBase;
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private VideoCapturer videoCapturer;
    private SurfaceTextureHelper surfaceTextureHelper;
    private VideoSource videoSource;
    private AudioSource audioSource;
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;

    private boolean peerJoined;
    private boolean offerSent;
    private boolean remoteDescriptionReady;
    private boolean callEnded;
    private boolean resourcesReleased;
    private boolean signalingReady;
    private boolean callJoinedSent;
    private boolean receiverRegistered;

    private final List<IceCandidate> pendingIceCandidates = new ArrayList<>();
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
                signalingReady = true;
                updateStatus("信令已连接，准备协商...");
                ensureCallJoinedSent();
                return;
            }
            if (WebRtcSignalingService.EVENT_DISCONNECTED.equals(kind)) {
                signalingReady = false;
                updateStatus("信令断开: " + (TextUtils.isEmpty(message) ? "unknown" : message));
                return;
            }
            if (WebRtcSignalingService.EVENT_ERROR.equals(kind)) {
                signalingReady = false;
                String error = TextUtils.isEmpty(message) ? "信令异常" : message;
                updateStatus(error);
                Toast.makeText(WebRtcCallActivity.this, error, Toast.LENGTH_SHORT).show();
                return;
            }
            if (WebRtcSignalingService.EVENT_MESSAGE.equals(kind) && !TextUtils.isEmpty(json)) {
                try {
                    handleSignalMessage(new JSONObject(json));
                } catch (JSONException e) {
                    updateStatus("信令消息解析失败");
                }
            }
        }
    };

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = true;
                for (Boolean value : result.values()) {
                    if (!Boolean.TRUE.equals(value)) {
                        granted = false;
                        break;
                    }
                }
                if (!granted) {
                    Toast.makeText(this, "通话需要相机和录音权限", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                startCallFlow();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webrtc_call);
        parseExtras();
        if (TextUtils.isEmpty(selfId) || TextUtils.isEmpty(peerId) || TextUtils.isEmpty(callId) || TextUtils.isEmpty(signalUrl)) {
            Toast.makeText(this, "通话参数缺失", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setTitle("WebRTC Call");
        bindViews();
        initRenderers();
        initListeners();
        registerSignalingReceiver();
        ensurePermissions();
    }

    private void parseExtras() {
        selfId = getIntent().getStringExtra(EXTRA_SELF_ID);
        peerId = getIntent().getStringExtra(EXTRA_PEER_ID);
        callId = getIntent().getStringExtra(EXTRA_CALL_ID);
        signalUrl = getIntent().getStringExtra(EXTRA_SIGNAL_URL);
        caller = getIntent().getBooleanExtra(EXTRA_IS_CALLER, false);
    }

    private void bindViews() {
        localRenderer = findViewById(R.id.rendererLocal);
        remoteRenderer = findViewById(R.id.rendererRemote);
        tvStatus = findViewById(R.id.tvWebRtcCallStatus);
        btnMute = findViewById(R.id.btnWebRtcMute);
        btnCamera = findViewById(R.id.btnWebRtcCamera);
        btnHangup = findViewById(R.id.btnWebRtcHangup);
        updateStatus("初始化中...");
    }

    private void initRenderers() {
        eglBase = EglBase.create();
        localRenderer.init(eglBase.getEglBaseContext(), null);
        localRenderer.setMirror(true);
        localRenderer.setEnableHardwareScaler(true);
        remoteRenderer.init(eglBase.getEglBaseContext(), null);
        remoteRenderer.setMirror(false);
        remoteRenderer.setEnableHardwareScaler(true);
    }

    private void initListeners() {
        btnMute.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (localAudioTrack != null) {
                localAudioTrack.setEnabled(!isChecked);
            }
        });
        btnCamera.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (localVideoTrack != null) {
                localVideoTrack.setEnabled(!isChecked);
            }
        });
        btnHangup.setOnClickListener(v -> hangupAndFinish("已挂断", true));
    }

    private void ensurePermissions() {
        boolean cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        boolean audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        if (cameraGranted && audioGranted) {
            startCallFlow();
        } else {
            permissionLauncher.launch(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO});
        }
    }

    private void startCallFlow() {
        ensureSignalingServiceStarted();
        initPeerConnectionFactory();
        initPeerConnection();
        initLocalTracks();
        updateStatus(caller ? "等待信令连接并协商..." : "等待对方发起协商...");
    }

    private void initPeerConnectionFactory() {
        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(this)
                        .setEnableInternalTracer(false)
                        .createInitializationOptions()
        );
        DefaultVideoEncoderFactory encoderFactory =
                new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true);
        DefaultVideoDecoderFactory decoderFactory =
                new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();
    }

    private void initPeerConnection() {
        List<PeerConnection.IceServer> iceServers = buildIceServers();
        PeerConnection.RTCConfiguration rtcConfiguration = new PeerConnection.RTCConfiguration(iceServers);
        rtcConfiguration.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
        rtcConfiguration.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfiguration, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                Log.d(TAG, "onSignalingChange: " + signalingState);
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d(TAG, "onIceConnectionChange: " + iceConnectionState);
                if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED
                        || iceConnectionState == PeerConnection.IceConnectionState.COMPLETED) {
                    runOnUiThread(() -> updateStatus("通话已连接"));
                } else if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED
                        || iceConnectionState == PeerConnection.IceConnectionState.FAILED
                        || iceConnectionState == PeerConnection.IceConnectionState.CLOSED) {
                    runOnUiThread(() -> updateStatus("连接已断开"));
                }
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                sendIceCandidate(iceCandidate);
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                if (mediaStream == null || mediaStream.videoTracks.isEmpty()) {
                    return;
                }
                VideoTrack videoTrack = mediaStream.videoTracks.get(0);
                runOnUiThread(() -> videoTrack.addSink(remoteRenderer));
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
            }

            @Override
            public void onDataChannel(org.webrtc.DataChannel dataChannel) {
            }

            @Override
            public void onRenegotiationNeeded() {
            }

            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
            }

            @Override
            public void onTrack(RtpTransceiver transceiver) {
                MediaStreamTrack track = transceiver.getReceiver().track();
                if (!(track instanceof VideoTrack)) {
                    return;
                }
                VideoTrack remoteTrack = (VideoTrack) track;
                runOnUiThread(() -> remoteTrack.addSink(remoteRenderer));
            }
        });
    }

    private List<PeerConnection.IceServer> buildIceServers() {
        List<PeerConnection.IceServer> servers = new ArrayList<>();
        servers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());

        String host = null;
        try {
            host = android.net.Uri.parse(signalUrl).getHost();
        } catch (Exception ignored) {
        }
        if (!TextUtils.isEmpty(host)) {
            servers.add(PeerConnection.IceServer.builder("turn:" + host + ":3478?transport=udp")
                    .setUsername(DEFAULT_TURN_USER)
                    .setPassword(DEFAULT_TURN_PASSWORD)
                    .createIceServer());
            servers.add(PeerConnection.IceServer.builder("turn:" + host + ":3478?transport=tcp")
                    .setUsername(DEFAULT_TURN_USER)
                    .setPassword(DEFAULT_TURN_PASSWORD)
                    .createIceServer());
        }
        return servers;
    }

    private void initLocalTracks() {
        videoCapturer = createVideoCapturer();
        if (videoCapturer == null) {
            Toast.makeText(this, "未找到可用摄像头", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        surfaceTextureHelper = SurfaceTextureHelper.create("WebRtcCaptureThread", eglBase.getEglBaseContext());
        videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(surfaceTextureHelper, getApplicationContext(), videoSource.getCapturerObserver());
        try {
            videoCapturer.startCapture(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_FPS);
        } catch (Exception e) {
            Log.e(TAG, "startCapture failed", e);
            Toast.makeText(this, "摄像头启动失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        localVideoTrack = peerConnectionFactory.createVideoTrack("local_video_track", videoSource);
        localVideoTrack.addSink(localRenderer);

        audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
        localAudioTrack = peerConnectionFactory.createAudioTrack("local_audio_track", audioSource);

        if (peerConnection != null) {
            peerConnection.addTrack(localVideoTrack);
            peerConnection.addTrack(localAudioTrack);
        }
    }

    private VideoCapturer createVideoCapturer() {
        Camera2Enumerator enumerator = new Camera2Enumerator(this);
        String[] deviceNames = enumerator.getDeviceNames();
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                CameraVideoCapturer capturer = enumerator.createCapturer(deviceName, null);
                if (capturer != null) {
                    return capturer;
                }
            }
        }
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                CameraVideoCapturer capturer = enumerator.createCapturer(deviceName, null);
                if (capturer != null) {
                    return capturer;
                }
            }
        }
        return null;
    }

    private void ensureSignalingServiceStarted() {
        Intent intent = new Intent(this, WebRtcSignalingService.class);
        intent.setAction(WebRtcSignalingService.ACTION_START_SIGNALING);
        intent.putExtra(WebRtcSignalingService.EXTRA_SELF_ID, selfId);
        intent.putExtra(WebRtcSignalingService.EXTRA_SIGNAL_URL, signalUrl);
        ContextCompat.startForegroundService(this, intent);
    }

    private void ensureCallJoinedSent() {
        if (callJoinedSent || callEnded || !signalingReady) {
            return;
        }
        sendSimpleSignal(WebRtcSignalTypes.CALL_JOINED, null);
        callJoinedSent = true;
        if (caller) {
            maybeCreateOffer();
        }
    }

    private void handleSignalMessage(JSONObject message) {
        String type = message.optString("type");
        String from = message.optString("from");
        String incomingCallId = message.optString("callId");
        if (!TextUtils.isEmpty(incomingCallId) && !incomingCallId.equals(callId)) {
            return;
        }
        if (!TextUtils.isEmpty(from) && !from.equals(peerId)) {
            return;
        }
        switch (type) {
            case WebRtcSignalTypes.BIND_OK:
                break;
            case WebRtcSignalTypes.CALL_JOINED:
                peerJoined = true;
                updateStatus("对方已进入通话，开始协商...");
                maybeCreateOffer();
                break;
            case WebRtcSignalTypes.OFFER:
                if (!caller) {
                    onRemoteOffer(message.optJSONObject("payload"));
                }
                break;
            case WebRtcSignalTypes.ANSWER:
                if (caller) {
                    onRemoteAnswer(message.optJSONObject("payload"));
                }
                break;
            case WebRtcSignalTypes.ICE_CANDIDATE:
                onRemoteIceCandidate(message.optJSONObject("payload"));
                break;
            case WebRtcSignalTypes.HANGUP:
                hangupAndFinish("对方已挂断", false);
                break;
            case WebRtcSignalTypes.ERROR:
                JSONObject payload = message.optJSONObject("payload");
                String err = payload == null ? "信令错误" : payload.optString("message", "信令错误");
                updateStatus(err);
                break;
            default:
                break;
        }
    }

    private void maybeCreateOffer() {
        if (!caller || offerSent || !peerJoined || peerConnection == null || callEnded) {
            return;
        }
        offerSent = true;
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        peerConnection.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new SimpleSdpObserver() {
                    @Override
                    public void onSetSuccess() {
                        JSONObject payload = new JSONObject();
                        try {
                            payload.put("sdp", sessionDescription.description);
                        } catch (JSONException e) {
                            Log.e(TAG, "offer payload build failed", e);
                        }
                        sendSimpleSignal(WebRtcSignalTypes.OFFER, payload);
                        updateStatus("已发送 Offer，等待对方应答...");
                    }
                }, sessionDescription);
            }

            @Override
            public void onCreateFailure(String s) {
                updateStatus("创建 Offer 失败: " + s);
            }
        }, constraints);
    }

    private void onRemoteOffer(JSONObject payload) {
        if (peerConnection == null || payload == null) {
            return;
        }
        String sdp = payload.optString("sdp");
        if (TextUtils.isEmpty(sdp)) {
            return;
        }
        SessionDescription remoteOffer = new SessionDescription(SessionDescription.Type.OFFER, sdp);
        peerConnection.setRemoteDescription(new SimpleSdpObserver() {
            @Override
            public void onSetSuccess() {
                remoteDescriptionReady = true;
                drainPendingIceCandidates();
                createAndSendAnswer();
            }

            @Override
            public void onSetFailure(String s) {
                updateStatus("设置远端 Offer 失败: " + s);
            }
        }, remoteOffer);
    }

    private void createAndSendAnswer() {
        if (peerConnection == null) {
            return;
        }
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        peerConnection.createAnswer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new SimpleSdpObserver() {
                    @Override
                    public void onSetSuccess() {
                        JSONObject payload = new JSONObject();
                        try {
                            payload.put("sdp", sessionDescription.description);
                        } catch (JSONException e) {
                            Log.e(TAG, "answer payload build failed", e);
                        }
                        sendSimpleSignal(WebRtcSignalTypes.ANSWER, payload);
                        updateStatus("已发送 Answer，建立连接中...");
                    }
                }, sessionDescription);
            }

            @Override
            public void onCreateFailure(String s) {
                updateStatus("创建 Answer 失败: " + s);
            }
        }, constraints);
    }

    private void onRemoteAnswer(JSONObject payload) {
        if (peerConnection == null || payload == null) {
            return;
        }
        String sdp = payload.optString("sdp");
        if (TextUtils.isEmpty(sdp)) {
            return;
        }
        SessionDescription remoteAnswer = new SessionDescription(SessionDescription.Type.ANSWER, sdp);
        peerConnection.setRemoteDescription(new SimpleSdpObserver() {
            @Override
            public void onSetSuccess() {
                remoteDescriptionReady = true;
                drainPendingIceCandidates();
                updateStatus("对方已应答，连接中...");
            }

            @Override
            public void onSetFailure(String s) {
                updateStatus("设置远端 Answer 失败: " + s);
            }
        }, remoteAnswer);
    }

    private void onRemoteIceCandidate(JSONObject payload) {
        if (peerConnection == null || payload == null) {
            return;
        }
        String candidate = payload.optString("candidate");
        String sdpMid = payload.optString("sdpMid");
        int sdpMLineIndex = payload.optInt("sdpMLineIndex", -1);
        if (TextUtils.isEmpty(candidate) || TextUtils.isEmpty(sdpMid) || sdpMLineIndex < 0) {
            return;
        }
        IceCandidate iceCandidate = new IceCandidate(sdpMid, sdpMLineIndex, candidate);
        if (remoteDescriptionReady) {
            peerConnection.addIceCandidate(iceCandidate);
        } else {
            pendingIceCandidates.add(iceCandidate);
        }
    }

    private void drainPendingIceCandidates() {
        if (peerConnection == null || pendingIceCandidates.isEmpty()) {
            return;
        }
        for (IceCandidate candidate : pendingIceCandidates) {
            peerConnection.addIceCandidate(candidate);
        }
        pendingIceCandidates.clear();
    }

    private void sendIceCandidate(IceCandidate iceCandidate) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("candidate", iceCandidate.sdp);
            payload.put("sdpMid", iceCandidate.sdpMid);
            payload.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
            sendSimpleSignal(WebRtcSignalTypes.ICE_CANDIDATE, payload);
        } catch (JSONException e) {
            Log.e(TAG, "sendIceCandidate failed", e);
        }
    }

    private void sendSimpleSignal(@NonNull String type, JSONObject payload) {
        if (!signalingReady) {
            return;
        }
        Intent intent = new Intent(this, WebRtcSignalingService.class);
        intent.setAction(WebRtcSignalingService.ACTION_SEND_SIGNAL);
        intent.putExtra(WebRtcSignalingService.EXTRA_SIGNAL_TYPE, type);
        intent.putExtra(WebRtcSignalingService.EXTRA_TO, peerId);
        intent.putExtra(WebRtcSignalingService.EXTRA_CALL_ID, callId);
        if (payload != null) {
            intent.putExtra(WebRtcSignalingService.EXTRA_PAYLOAD_JSON, payload.toString());
        }
        startService(intent);
    }

    private void hangupAndFinish(String tip, boolean notifyPeer) {
        if (callEnded) {
            return;
        }
        callEnded = true;
        if (notifyPeer) {
            sendSimpleSignal(WebRtcSignalTypes.HANGUP, null);
        }
        updateStatus(tip);
        Toast.makeText(this, tip, Toast.LENGTH_SHORT).show();
        releaseResources();
        finish();
    }

    private void updateStatus(String message) {
        tvStatus.setText("状态: " + message);
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

    private void releaseResources() {
        if (resourcesReleased) {
            return;
        }
        resourcesReleased = true;
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (Exception ignored) {
            }
            videoCapturer.dispose();
            videoCapturer = null;
        }
        if (peerConnection != null) {
            peerConnection.close();
            peerConnection.dispose();
            peerConnection = null;
        }
        if (localVideoTrack != null) {
            localVideoTrack.dispose();
            localVideoTrack = null;
        }
        if (localAudioTrack != null) {
            localAudioTrack.dispose();
            localAudioTrack = null;
        }
        if (videoSource != null) {
            videoSource.dispose();
            videoSource = null;
        }
        if (audioSource != null) {
            audioSource.dispose();
            audioSource = null;
        }
        if (surfaceTextureHelper != null) {
            surfaceTextureHelper.dispose();
            surfaceTextureHelper = null;
        }
        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
            peerConnectionFactory = null;
        }
        if (localRenderer != null) {
            localRenderer.release();
        }
        if (remoteRenderer != null) {
            remoteRenderer.release();
        }
        if (eglBase != null) {
            eglBase.release();
            eglBase = null;
        }
    }

    @Override
    protected void onDestroy() {
        if (!callEnded) {
            callEnded = true;
            sendSimpleSignal(WebRtcSignalTypes.HANGUP, null);
        }
        unregisterSignalingReceiver();
        releaseResources();
        super.onDestroy();
    }

    private static class SimpleSdpObserver implements SdpObserver {
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
        }

        @Override
        public void onSetSuccess() {
        }

        @Override
        public void onCreateFailure(String s) {
        }

        @Override
        public void onSetFailure(String s) {
        }
    }
}
