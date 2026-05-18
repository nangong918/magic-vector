package com.vectordemo.domain.platform

enum class PlatformType {
    ANDROID,
    IOS,
}

expect object PlatformFeatures {
    val platformType: PlatformType
    val supportsVoiceAgent: Boolean
    val supportsLiveStreaming: Boolean
    val supportsKni: Boolean
}
