package com.magicvector.manager.audio.vad

interface VadDetectionCallback {
    fun onStartSpeech(audioBuffer: ByteArray)
    fun speeching(audioBuffer: ByteArray)
    fun onStopSpeech()
}