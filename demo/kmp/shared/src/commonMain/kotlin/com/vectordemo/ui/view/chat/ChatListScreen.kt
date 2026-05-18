package com.vectordemo.ui.view.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vectordemo.viewModel.chat.ChatListMessage
import com.vectordemo.viewModel.chat.ChatListUiState

@Composable
fun ChatListScreen(
    state: ChatListUiState,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("ChatList Demo")
            Button(onClick = onBack) { Text("返回") }
        }
        Text(state.status.name, color = if (state.errorMessage.isNotBlank()) Color.Red else Color.Gray)
        if (state.errorMessage.isNotBlank()) Text(state.errorMessage, color = Color.Red)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.messages, key = { it.id }) { msg -> ChatBubble(msg) }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("输入消息") },
            )
            Button(onClick = {
                val text = input.trim()
                if (text.isNotEmpty()) {
                    input = ""
                    onSend(text)
                }
            }) { Text("发送") }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatListMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .background(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(16.dp))
                .padding(12.dp),
        ) {
            Text(if (isUser) "你" else "AI", color = Color.Gray)
            Text(message.text)
        }
    }
}
