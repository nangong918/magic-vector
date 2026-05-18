package com.vectordemo.domain.platform

actual object PlatformFeatures {
    actual val platformType: PlatformType = PlatformType.IOS
    actual val supportsVoiceAgent: Boolean = false
    actual val supportsLiveStreaming: Boolean = false
    actual val supportsKni: Boolean = false
}
