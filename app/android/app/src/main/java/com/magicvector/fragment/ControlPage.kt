package com.magicvector.fragment

import android.view.SurfaceView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.magicvector.viewModel.fragment.ControlEffect
import com.magicvector.viewModel.fragment.ControlIntent
import com.magicvector.viewModel.fragment.ControlPlatform
import com.magicvector.viewModel.fragment.ControlUiState
import com.magicvector.viewModel.fragment.ControlVm
import com.magicvector.viewModel.fragment.StreamSource
import com.magicvector.viewModel.fragment.StreamTestMode
import kotlin.math.roundToInt

@Composable
fun ControlScreen(
    modifier: Modifier = Modifier,
    viewModel: ControlVm = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.processIntent(ControlIntent.Initialize)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ControlEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    ControlContent(
        modifier = modifier,
        state = state,
        onPlatformSwitch = { viewModel.processIntent(ControlIntent.SwitchPlatform(it)) },
        onStreamSourceSwitch = { viewModel.processIntent(ControlIntent.SwitchStreamSource(it)) },
        onRefreshStatus = { viewModel.processIntent(ControlIntent.FetchControlStatus) },
        onReconnect = { viewModel.processIntent(ControlIntent.ReconnectControlWs) },
        onLeftJoystick = { x, y -> viewModel.processIntent(ControlIntent.LeftJoystickDrag(x, y)) },
        onRightJoystick = { x, y -> viewModel.processIntent(ControlIntent.RightJoystickDrag(x, y)) },
        onForward = { viewModel.processIntent(ControlIntent.SendQuickCommandForward) },
        onStop = { viewModel.processIntent(ControlIntent.SendQuickCommandStop) },
        onToggleRecording = { viewModel.processIntent(ControlIntent.ToggleRecording) },
        onSwitchTestMode = { viewModel.processIntent(ControlIntent.SwitchTestMode(it)) },
        onUpdateRtmpUrl = { viewModel.processIntent(ControlIntent.UpdateTestRtmpUrl(it)) },
        onStartTest = { viewModel.processIntent(ControlIntent.StartTestStream) },
        onStopTest = { viewModel.processIntent(ControlIntent.StopTestStream) }
    )
}

@Composable
private fun ControlContent(
    modifier: Modifier = Modifier,
    state: ControlUiState,
    onPlatformSwitch: (ControlPlatform) -> Unit,
    onStreamSourceSwitch: (StreamSource) -> Unit,
    onRefreshStatus: () -> Unit,
    onReconnect: () -> Unit,
    onLeftJoystick: (Float, Float) -> Unit,
    onRightJoystick: (Float, Float) -> Unit,
    onForward: () -> Unit,
    onStop: () -> Unit,
    onToggleRecording: () -> Unit,
    onSwitchTestMode: (StreamTestMode) -> Unit,
    onUpdateRtmpUrl: (String) -> Unit,
    onStartTest: () -> Unit,
    onStopTest: () -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            DeviceStatusCard(
                state = state,
                onRefreshStatus = onRefreshStatus,
                onReconnect = onReconnect
            )
        }
        item {
            PlatformSelectCard(
                state = state,
                onPlatformSwitch = onPlatformSwitch,
                onStreamSourceSwitch = onStreamSourceSwitch
            )
        }
        item {
            VideoPanelCard(state = state)
        }
        item {
            CommandPanelCard(
                onLeftJoystick = onLeftJoystick,
                onRightJoystick = onRightJoystick,
                onForward = onForward,
                onStop = onStop
            )
        }
        item {
            RecordCard(state = state, onToggleRecording = onToggleRecording)
        }
        item {
            AppRtmpTestCard(
                state = state,
                onSwitchTestMode = onSwitchTestMode,
                onUpdateRtmpUrl = onUpdateRtmpUrl,
                onStartTest = onStartTest,
                onStopTest = onStopTest
            )
        }
    }
}

@Composable
private fun DeviceStatusCard(
    state: ControlUiState,
    onRefreshStatus: () -> Unit,
    onReconnect: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF101828), contentColor = Color.White)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "设备连接状态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatusChip("App-Spring", state.appToSpringConnected)
                StatusChip("RK-Spring", state.rkToSpringConnected)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatusChip("RK-App WiFi", state.appToRkWifiConnected)
                StatusChip("RK-App BLE", state.appToRkBleConnected)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatusChip("Control WS", state.controlWsConnected)
                StatusChip("网络在线", state.networkOnline)
            }
            Text(text = "RK Agent: ${state.rkAgentMode}", style = MaterialTheme.typography.bodySmall)
            if (state.wsReconnecting) {
                Text(
                    text = "重连中... 第 ${state.wsRetryCount} 次",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFD166)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRefreshStatus) { Text("刷新状态") }
                Button(onClick = onReconnect) { Text("重连WS") }
            }
        }
    }
}

@Composable
private fun StatusChip(title: String, ok: Boolean) {
    val bg = if (ok) Color(0xFF0E9F6E) else Color(0xFFB42318)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = "$title ${if (ok) "已连接" else "未连接"}", color = Color.White, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PlatformSelectCard(
    state: ControlUiState,
    onPlatformSwitch: (ControlPlatform) -> Unit,
    onStreamSourceSwitch: (StreamSource) -> Unit
) {
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "控制平台", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ControlPlatform.entries.forEach { platform ->
                    FilterChip(
                        selected = state.platform == platform,
                        onClick = { onPlatformSwitch(platform) },
                        label = { Text(platform.label) }
                    )
                }
            }
            Text(text = "视频来源", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StreamSource.entries.forEach { source ->
                    FilterChip(
                        selected = state.streamSource == source,
                        onClick = { onStreamSourceSwitch(source) },
                        label = { Text(source.label) }
                    )
                }
            }
            if (state.platform == ControlPlatform.OFFLINE_BLE) {
                Text(
                    text = "BLE 模式下仅支持遥控指令，不提供实时视频（TODO: RK BLE链路）",
                    color = Color(0xFFB45309),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun VideoPanelCard(state: ControlUiState) {
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "实时视频", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (state.streamEnabled) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            SurfaceView(context).apply {
                                setZOrderOnTop(false)
                            }
                        }
                    )
                    Text(
                        text = "TODO: 接入RTMP/UDP渲染（SurfaceView）",
                        color = Color.White
                    )
                } else {
                    Text(text = "BLE模式不提供实时视频", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun CommandPanelCard(
    onLeftJoystick: (Float, Float) -> Unit,
    onRightJoystick: (Float, Float) -> Unit,
    onForward: () -> Unit,
    onStop: () -> Unit
) {
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "控制指令", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                JoystickView(title = "方向", onOffsetChange = onLeftJoystick)
                JoystickView(title = "移动", onOffsetChange = onRightJoystick)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onForward) { Text("前进") }
                Button(onClick = onStop) { Text("急停") }
            }
        }
    }
}

@Composable
private fun JoystickView(
    title: String,
    onOffsetChange: (Float, Float) -> Unit
) {
    val maxOffset = 46f
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelMedium)
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .border(width = 2.dp, color = Color(0xFF98A2B3), shape = CircleShape)
                .background(Color(0xFFF2F4F7))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            offsetX = 0f
                            offsetY = 0f
                            onOffsetChange(0f, 0f)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount.x).coerceIn(-maxOffset, maxOffset)
                            offsetY = (offsetY + dragAmount.y).coerceIn(-maxOffset, maxOffset)
                            onOffsetChange(offsetX / maxOffset, offsetY / maxOffset)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1D2939))
            )
        }
    }
}

@Composable
private fun RecordCard(
    state: ControlUiState,
    onToggleRecording: () -> Unit
) {
    Card {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("本地录制", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (state.recording) "录制中 ${formatRecordTime(state.recordSeconds)}" else "未录制",
                    color = if (state.recording) Color(0xFFB42318) else Color(0xFF475467)
                )
            }
            Button(onClick = onToggleRecording) {
                Text(if (state.recording) "停止录制" else "开始录制")
            }
        }
    }
}

@Composable
private fun AppRtmpTestCard(
    state: ControlUiState,
    onSwitchTestMode: (StreamTestMode) -> Unit,
    onUpdateRtmpUrl: (String) -> Unit,
    onStartTest: () -> Unit,
    onStopTest: () -> Unit
) {
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "App 推拉流测试", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StreamTestMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.testMode == mode,
                        onClick = { onSwitchTestMode(mode) },
                        label = { Text(mode.label) }
                    )
                }
            }
            OutlinedTextField(
                value = state.testRtmpUrl,
                onValueChange = onUpdateRtmpUrl,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("RTMP Url") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStartTest) { Text(if (state.testMode == StreamTestMode.PUSH) "开始推流" else "开始拉流") }
                Button(onClick = onStopTest) { Text("停止") }
            }
            Text(
                text = if (state.testStreaming) "测试中..." else "未启动",
                color = if (state.testStreaming) Color(0xFF0E9F6E) else Color(0xFF475467)
            )
        }
    }
}

private fun formatRecordTime(seconds: Long): String {
    val minute = seconds / 60
    val second = seconds % 60
    return String.format("%02d:%02d", minute, second)
}

@Preview(showBackground = true, widthDp = 380, heightDp = 900)
@Composable
private fun ControlContentPreview() {
    ControlContent(
        state = ControlUiState(
            appToSpringConnected = true,
            rkToSpringConnected = false,
            appToRkWifiConnected = true,
            controlWsConnected = true,
            recording = true,
            recordSeconds = 72
        ),
        onPlatformSwitch = {},
        onStreamSourceSwitch = {},
        onRefreshStatus = {},
        onReconnect = {},
        onLeftJoystick = { _, _ -> },
        onRightJoystick = { _, _ -> },
        onForward = {},
        onStop = {},
        onToggleRecording = {},
        onSwitchTestMode = {},
        onUpdateRtmpUrl = {},
        onStartTest = {},
        onStopTest = {}
    )
}
