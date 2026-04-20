package com.vectordemo.service.ai

import android.content.Context

object PromptAssetLoader {
    fun loadSystemPrompt(context: Context): String {
        return context.assets.open("txt/clt_agent.txt").bufferedReader().use { it.readText() }
    }
}

