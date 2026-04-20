package com.vectordemo.service.ai

interface ChatService {
    suspend fun sendChat(
        systemPrompt: String,
        history: List<Map<String, String>>,
        userMessage: String,
        onDelta: (String) -> Unit,
        onDone: () -> Unit
    )
}

