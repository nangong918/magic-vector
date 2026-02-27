package com.example.flutteraar.config;

import android.content.Context;

import com.demo.aarlib.voicewakeup.VoiceWakeUpBridge;

import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ModuleKeyConfigLoader {
    private ModuleKeyConfigLoader() {
    }

    public static VoiceWakeUpBridge.Config loadOfflineIvwConfig(Context context) {
        try (InputStream input = context.getAssets().open("module_key.json")) {
            byte[] data = new byte[input.available()];
            int read = input.read(data);
            if (read <= 0) {
                return null;
            }
            String jsonText = new String(data, StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(jsonText);
            JSONObject xfyun = root.optJSONObject("xfyun");
            if (xfyun == null) {
                return null;
            }
            JSONObject offlineIvw = xfyun.optJSONObject("offlineIvw");
            if (offlineIvw == null) {
                return null;
            }
            String appId = offlineIvw.optString("appId", "").trim();
            String apiKey = offlineIvw.optString("apiKey", "").trim();
            String apiSecret = offlineIvw.optString("apiSecret", "").trim();
            String abilityId = offlineIvw.optString("abilityId", "").trim();
            if (appId.isEmpty() || apiKey.isEmpty() || apiSecret.isEmpty() || abilityId.isEmpty()) {
                return null;
            }
            return new VoiceWakeUpBridge.Config(appId, apiKey, apiSecret, "", abilityId);
        } catch (Exception ignored) {
            return null;
        }
    }
}
