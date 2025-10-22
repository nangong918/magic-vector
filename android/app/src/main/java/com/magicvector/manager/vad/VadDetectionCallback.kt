package com.magicvector.manager.vad

interface VadDetectionCallback {
    fun isSpeech(audioData: ShortArray)
    fun isSilence()
}