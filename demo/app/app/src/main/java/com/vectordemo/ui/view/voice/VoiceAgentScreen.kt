package com.vectordemo.ui.view.voice

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAgentScreen(
    state: VoiceAgentUiState,
    serviceStatusText: String,
    onBack: () -> Unit,
    onSelectLlm: (VoiceAgentLlmProvider) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Agent") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                val targetSize by animateDpAsState(targetValue = phaseSize(state.phase), label = "ball-size")
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(targetSize)
                            .background(phaseColor(state.phase), shape = CircleShape)
                    )
                }

                Text(
                    text = phaseText(state.phase),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .padding(12.dp)
                        .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                        Text("控制台日志", style = MaterialTheme.typography.titleSmall)
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 8.dp)
                        ) {
                            itemsIndexed(state.logs) { _, item ->
                                Text(item, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            LlmDropdown(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                selected = state.llmProvider,
                onSelect = onSelectLlm
            )

            Text(
                text = serviceStatusText,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun LlmDropdown(
    modifier: Modifier = Modifier,
    selected: VoiceAgentLlmProvider,
    onSelect: (VoiceAgentLlmProvider) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (selected == VoiceAgentLlmProvider.ALI) "LLM: 阿里百炼" else "LLM: 科大讯飞",
                color = Color.White
            )
            IconButton(onClick = { expanded = true }) {
                Text("▼", color = Color.White)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("LLM: 阿里百炼") },
                onClick = {
                    expanded = false
                    onSelect(VoiceAgentLlmProvider.ALI)
                }
            )
            DropdownMenuItem(
                text = { Text("LLM: 科大讯飞") },
                onClick = {
                    expanded = false
                    onSelect(VoiceAgentLlmProvider.XFYUN)
                }
            )
        }
    }
}

private fun phaseColor(phase: VoiceAgentPhase): Color = when (phase) {
    VoiceAgentPhase.READY -> Color(0xFF4CAF50)
    VoiceAgentPhase.WAKE_DETECTED_WAITING_SPEECH, VoiceAgentPhase.USER_SPEAKING, VoiceAgentPhase.USER_SPEECH_ENDED -> Color(0xFF2196F3)
    VoiceAgentPhase.AGENT_REPLYING -> Color(0xFF9C27B0)
    VoiceAgentPhase.ERROR, VoiceAgentPhase.INITIALIZING -> Color(0xFFF44336)
}

private fun phaseSize(phase: VoiceAgentPhase) = when (phase) {
    VoiceAgentPhase.USER_SPEAKING -> 180.dp
    VoiceAgentPhase.AGENT_REPLYING -> 162.dp
    else -> 150.dp
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

