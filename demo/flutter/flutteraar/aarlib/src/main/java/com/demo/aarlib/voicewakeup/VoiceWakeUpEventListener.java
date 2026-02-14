package com.demo.aarlib.voicewakeup;

import java.util.Map;

public interface VoiceWakeUpEventListener {
    void onEvent(Map<String, Object> payload);
}
