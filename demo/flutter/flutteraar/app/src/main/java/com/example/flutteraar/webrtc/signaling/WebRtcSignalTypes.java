package com.example.flutteraar.webrtc.signaling;

public final class WebRtcSignalTypes {
    private WebRtcSignalTypes() {
    }

    public static final String BIND_OK = "bind_ok";
    public static final String ERROR = "error";
    public static final String PING = "ping";
    public static final String PONG = "pong";

    public static final String CALL_INVITE = "call_invite";
    public static final String CALL_ACCEPT = "call_accept";
    public static final String CALL_REJECT = "call_reject";
    public static final String CALL_JOINED = "call_joined";
    public static final String HANGUP = "hangup";

    public static final String OFFER = "offer";
    public static final String ANSWER = "answer";
    public static final String ICE_CANDIDATE = "ice_candidate";
}
