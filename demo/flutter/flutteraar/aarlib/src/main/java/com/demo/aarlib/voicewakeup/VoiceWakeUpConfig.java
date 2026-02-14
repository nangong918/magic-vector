package com.demo.aarlib.voicewakeup;

public class VoiceWakeUpConfig {
    private final String appId;
    private final String apiKey;
    private final String apiSecret;
    private final String workDir;
    private final String abilityId;

    public VoiceWakeUpConfig(String appId, String apiKey, String apiSecret, String workDir, String abilityId) {
        this.appId = appId;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.workDir = workDir;
        this.abilityId = abilityId;
    }

    public String getAppId() {
        return appId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public String getWorkDir() {
        return workDir;
    }

    public String getAbilityId() {
        return abilityId;
    }
}
