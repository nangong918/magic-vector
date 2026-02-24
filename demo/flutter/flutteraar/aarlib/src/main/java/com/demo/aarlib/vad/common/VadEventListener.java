package com.demo.aarlib.vad.common;

import java.util.Map;

public interface VadEventListener {
    void onEvent(Map<String, Object> payload);
}
