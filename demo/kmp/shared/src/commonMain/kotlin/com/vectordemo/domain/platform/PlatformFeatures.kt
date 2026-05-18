package com.vectordemo.domain.platform

expect object PlatformFeatures {
    val supportsVoiceAgent: Boolean
    val supportsLiveStreaming: Boolean
    val supportsKni: Boolean
}
