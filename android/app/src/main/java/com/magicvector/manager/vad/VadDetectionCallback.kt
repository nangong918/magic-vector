package com.magicvector.manager.vad

interface VadDetectionCallback {
    fun onStartSpeech(audioBuffer: ByteArray)
    fun speeching(audioBuffer: ByteArray)
    fun onStopSpeech()
}