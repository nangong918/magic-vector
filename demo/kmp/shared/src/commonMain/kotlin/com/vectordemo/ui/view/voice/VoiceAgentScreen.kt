package com.vectordemo.ui.view.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vectordemo.viewModel.voice.VoiceAgentLlmProvider
import com.vectordemo.viewModel.voice.VoiceAgentPhase
import com.vectordemo.viewModel.voice.VoiceAgentUiState

@Composable
fun VoiceAgentScreen(
    state: VoiceAgentUiState,
    serviceStatusText: String,
    onBack: () -> Unit,
    onSelectLlm: (VoiceAgentLlmProvider) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Voice Agent")
            Button(onClick = onBack) { Text("返回") }
        }
        LlmDropdown(selected = state.llmProvider, onSelect = onSelectLlm)
        Text(serviceStatusText, modifier = Modifier.padding(vertical = 8.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(140.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.height(100.dp).fillMaxWidth(0.5f)
                    .background(phaseColor(state.phase), CircleShape),
            )
        }
        Text(phaseText(state.phase))
        Box(
            modifier = Modifier.fillMaxSize().padding(top = 8.dp)
                .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                itemsIndexed(state.logs) { _, item -> Text(item) }
            }
        }
    }
}

@Composable
private fun LlmDropdown(
    selected: VoiceAgentLlmProvider,
    onSelect: (VoiceAgentLlmProvider) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(if (selected == VoiceAgentLlmProvider.ALI) "LLM: 阿里百炼" else "LLM: 科大讯飞")
        Button(onClick = { expanded = true }) { Text("切换") }
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("LLM: 阿里百炼") },
            onClick = {
                expanded = false
                onSelect(VoiceAgentLlmProvider.ALI)
            },
        )
        DropdownMenuItem(
            text = { Text("LLM: 科大讯飞") },
            onClick = {
                expanded = false
                onSelect(VoiceAgentLlmProvider.XFYUN)
            },
        )
    }
}

private fun phaseColor(phase: VoiceAgentPhase): Color = when (phase) {
    VoiceAgentPhase.READY -> Color(0xFF4CAF50)
    VoiceAgentPhase.WAKE_DETECTED_WAITING_SPEECH,
    VoiceAgentPhase.USER_SPEAKING,
    VoiceAgentPhase.USER_SPEECH_ENDED -> Color(0xFF2196F3)
    VoiceAgentPhase.AGENT_REPLYING -> Color(0xFF9C27B0)
    VoiceAgentPhase.ERROR,
    VoiceAgentPhase.INITIALIZING -> Color(0xFFF44336)
}

private fun phaseText(phase: VoiceAgentPhase): String = when (phase) {
    VoiceAgentPhase.INITIALIZING -> "初始化中"
    VoiceAgentPhase.READY -> "就绪（仅唤醒监听中）"
    VoiceAgentPhase.WAKE_DETECTED_WAITING_SPEECH -> "已唤醒，等待用户开始说话"
    VoiceAgentPhase.USER_SPEAKING -> "唤醒后讲话中（VAD+STT）"
    VoiceAgentPhase.USER_SPEECH_ENDED -> "讲话结束，等待STT最终结果"
    VoiceAgentPhase.AGENT_REPLYING -> "Agent回复中"
    VoiceAgentPhase.ERROR -> "异常（自动恢复中）"
}
