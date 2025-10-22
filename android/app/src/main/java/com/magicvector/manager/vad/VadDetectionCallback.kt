package com.magicvector.manager.vad

interface VadDetectionCallback {
    fun onStartSpeech(audioData: ShortArray)
    fun speeching(audioData: ShortArray)
    fun onStopSpeech()
}