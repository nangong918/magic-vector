package com.magicvector.activity.test

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.magicvector.utils.permissions.GainPermissionCallback
import com.magicvector.utils.ui.ToastUtils
import com.magicvector.domain.test.AudioRecordPlayState
import com.magicvector.domain.test.ChatState
import com.magicvector.domain.test.RealtimeChatState
import com.magicvector.domain.test.TtsChatState
import com.magicvector.domain.test.WebsocketState
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.ui.view.test.RealtimeQuestionInput
import com.magicvector.ui.view.test.TestSectionCard
import com.magicvector.ui.view.test.TestStateLine
import com.magicvector.ui.view.test.TwoActionButtons
import com.magicvector.utils.activity.BaseComponentActivity
import com.magicvector.utils.permissions.ComposePermissionUtils
import com.magicvector.viewModel.activity.ComposeTestEffect
import com.magicvector.viewModel.activity.ComposeTestIntent
import com.magicvector.viewModel.activity.ComposeTestState
import com.magicvector.viewModel.activity.TestVm

class TestActivity : BaseComponentActivity() {
    private val vm: TestVm by viewModels()
    private val cameraPermissionForEmoji = ComposePermissionUtils()
    private val cameraPermissionForYolo = ComposePermissionUtils()
    private val recordPermissionForAudio = ComposePermissionUtils()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraPermissionForEmoji.registerPermissionLauncher(this, arrayOf(Manifest.permission.CAMERA))
        cameraPermissionForYolo.registerPermissionLauncher(this, arrayOf(Manifest.permission.CAMERA))
        recordPermissionForAudio.registerPermissionLauncher(
            this,
            mustPermissions = arrayOf(Manifest.permission.RECORD_AUDIO),
            optionalPermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        )
        enableEdgeToEdge()
        setContent {
            MagicVectorTheme {
                ComposeTestRoute(vm = vm)
            }
        }
    }

    @Composable
    private fun ComposeTestRoute(vm: TestVm) {
        val state by vm.uiState.collectAsState()
        val activity = this
        val withAudioPermission: (ComposeTestIntent) -> Unit = { grantedIntent ->
            recordPermissionForAudio.requestPermissions(activity, object : GainPermissionCallback {
                override fun allGranted() {
                    vm.processIntent(grantedIntent)
                }

                override fun notGranted(notGrantedPermissions: Array<String?>?) {
                    ToastUtils.showToastActivity(activity, "没有获取录音权限")
                }

                override fun always() {
                }
            })
        }

        LaunchedEffect(vm) {
            vm.effect.collect { effect ->
                when (effect) {
                    ComposeTestEffect.RequestCameraForEmojiTest -> {
                        cameraPermissionForEmoji.requestPermissions(activity, object : GainPermissionCallback {
                            override fun allGranted() {
                                vm.processIntent(ComposeTestIntent.CameraPermissionResultForEmojiTest(true))
                            }

                            override fun notGranted(notGrantedPermissions: Array<String?>?) {
                                vm.processIntent(ComposeTestIntent.CameraPermissionResultForEmojiTest(false))
                            }

                            override fun always() {
                            }
                        })
                    }
                    ComposeTestEffect.RequestCameraForYoloTest -> {
                        cameraPermissionForYolo.requestPermissions(activity, object : GainPermissionCallback {
                            override fun allGranted() {
                                vm.processIntent(ComposeTestIntent.CameraPermissionResultForYoloTest(true))
                            }

                            override fun notGranted(notGrantedPermissions: Array<String?>?) {
                                vm.processIntent(ComposeTestIntent.CameraPermissionResultForYoloTest(false))
                            }

                            override fun always() {
                            }
                        })
                    }
                    ComposeTestEffect.NavigateToEmojiTest -> {
                        startActivity(Intent(activity, AgentEmojiTestActivity::class.java))
                    }
                    ComposeTestEffect.NavigateToYoloTest -> {
                        startActivity(Intent(activity, YOLOv8Activity::class.java))
                    }
                    ComposeTestEffect.NavigateToVad -> {
                        startActivity(Intent(activity, VADMainActivity::class.java))
                    }
                    is ComposeTestEffect.ShowToast -> {
                        ToastUtils.showToastActivity(activity, effect.message)
                    }
                }
            }
        }

        ComposeTestScreen(
            state = state,
            onIntent = { vm.processIntent(it) },
            withAudioPermission = withAudioPermission
        )
    }
}

@Composable
private fun ComposeTestScreen(
    state: ComposeTestState,
    onIntent: (ComposeTestIntent) -> Unit,
    withAudioPermission: (ComposeTestIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        TestSectionCard("Emoji 测试", "跳转 Emoji 眼睛移动测试页") {
            Button(onClick = { onIntent(ComposeTestIntent.GoEmojiTest) }) { Text("跳转Emoji测试Activity") }
        }

        TestSectionCard("YOLOv8 测试", "跳转目标检测测试页") {
            Button(onClick = { onIntent(ComposeTestIntent.GoYoloTest) }) { Text("跳转YOLOv8目标检测测试Activity") }
        }

        TestSectionCard("VAD 测试", "跳转 VAD 语音活动测试页") {
            Button(onClick = { onIntent(ComposeTestIntent.GoVadTest) }) { Text("跳转VAD测试Activity") }
        }

        RealtimeChat2Section(state = state, onIntent = onIntent, withAudioPermission = withAudioPermission)
        RealtimeChatSection(state = state, onIntent = onIntent, withAudioPermission = withAudioPermission)
        AudioRecordSection(state = state, onIntent = onIntent, withAudioPermission = withAudioPermission)
        WebsocketSection(state = state, onIntent = onIntent)
        TtsSseSection(state = state, onIntent = onIntent)
        SseSection(state = state, onIntent = onIntent)
    }
}

@Composable
private fun RealtimeChat2Section(
    state: ComposeTestState,
    onIntent: (ComposeTestIntent) -> Unit,
    withAudioPermission: (ComposeTestIntent) -> Unit
) {
    TestSectionCard("Realtime Chat2", "实时语音 + 问答流测试2") {
        TwoActionButtons(
            leftText = "初始Realtime Chat2",
            rightText = if (state.realtimeChat2State is RealtimeChatState.RecordingAndSending) "结束录音 + 接收消息" else "开始录音 + 流式发送",
            onLeftClick = { withAudioPermission(ComposeTestIntent.InitRealtimeChat2) },
            onRightClick = { onIntent(ComposeTestIntent.ToggleRecordRealtimeChat2) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        RealtimeQuestionInput(
            value = state.realtimeChat2Question,
            onValueChange = { onIntent(ComposeTestIntent.UpdateRealtime2Question(it)) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TwoActionButtons(
            leftText = "发送Question",
            rightText = "停止聊天",
            onLeftClick = { onIntent(ComposeTestIntent.SendRealtimeChat2Question) },
            onRightClick = { onIntent(ComposeTestIntent.StopRealtimeChat2) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TestStateLine("状态: ", getRealtimeStateText(state.realtimeChat2State))
        TestStateLine("消息: ", state.realtimeChat2Message.ifBlank { "暂无消息" })
        TestStateLine("音量: ", String.format("%.2f", state.realtimeChat2Volume))
    }
}

@Composable
private fun RealtimeChatSection(
    state: ComposeTestState,
    onIntent: (ComposeTestIntent) -> Unit,
    withAudioPermission: (ComposeTestIntent) -> Unit
) {
    TestSectionCard("Realtime Chat", "实时语音聊天测试") {
        TwoActionButtons(
            leftText = "初始Realtime Chat",
            rightText = if (state.realtimeChatState is RealtimeChatState.RecordingAndSending) "结束录音 + 接收消息" else "开始录音 + 流式发送",
            onLeftClick = { withAudioPermission(ComposeTestIntent.InitRealtimeChat) },
            onRightClick = { onIntent(ComposeTestIntent.ToggleRecordRealtimeChat) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onIntent(ComposeTestIntent.StopRealtimeChat) }) { Text("停止聊天") }
        Spacer(modifier = Modifier.height(8.dp))
        TestStateLine("状态: ", getRealtimeStateText(state.realtimeChatState))
        TestStateLine("消息: ", state.realtimeChatMessage.ifBlank { "暂无消息" })
        TestStateLine("音量: ", String.format("%.2f", state.realtimeChatVolume))
    }
}

@Composable
private fun AudioRecordSection(
    state: ComposeTestState,
    onIntent: (ComposeTestIntent) -> Unit,
    withAudioPermission: (ComposeTestIntent) -> Unit
) {
    TestSectionCard("录音测试", "AudioRecord/AudioTrack 本地录播测试") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { withAudioPermission(ComposeTestIntent.InitRecordAudio) }) { Text("初始化录音") }
            Button(onClick = { onIntent(ComposeTestIntent.BeginRecordAudio) }) { Text("开始录音") }
            Button(onClick = { onIntent(ComposeTestIntent.StopRecordAudio) }) { Text("停止录音") }
            Button(onClick = { onIntent(ComposeTestIntent.PlayRecordAudio) }) { Text("播放录音") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        TestStateLine("状态: ", getAudioStateText(state.audioRecordPlayState))
        TestStateLine("音量: ", String.format("%.2f", state.audioRecordVolume))
    }
}

@Composable
private fun WebsocketSection(
    state: ComposeTestState,
    onIntent: (ComposeTestIntent) -> Unit
) {
    TestSectionCard("WebSocket 测试", "WebSocket 连接、发送与断开") {
        TwoActionButtons(
            leftText = "初始化 + 连接",
            rightText = "发送消息",
            onLeftClick = { onIntent(ComposeTestIntent.ConnectWebsocket) },
            onRightClick = { onIntent(ComposeTestIntent.SendWebsocketMessage) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onIntent(ComposeTestIntent.DisconnectWebsocket) }) { Text("断开websocket") }
        Spacer(modifier = Modifier.height(8.dp))
        TestStateLine("状态: ", getWebsocketStateText(state.websocketState))
        TestStateLine("记录: ", state.websocketMessageHistory.ifBlank { "暂未收到消息" })
    }
}

@Composable
private fun TtsSseSection(
    state: ComposeTestState,
    onIntent: (ComposeTestIntent) -> Unit
) {
    TestSectionCard("TTS SSE 测试", "文本 + 音频流 SSE") {
        TwoActionButtons(
            leftText = "初始化AudioTrack",
            rightText = "发送TTS SSE消息",
            onLeftClick = { onIntent(ComposeTestIntent.InitializeTtsAudio) },
            onRightClick = { onIntent(ComposeTestIntent.SendTtsMessage) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TestStateLine("状态: ", getTtsStateText(state.ttsSseState))
        TestStateLine("回复: ", state.ttsSseMessage.ifBlank { "暂未收到消息" })
    }
}

@Composable
private fun SseSection(
    state: ComposeTestState,
    onIntent: (ComposeTestIntent) -> Unit
) {
    TestSectionCard("SSE 测试", "普通文本流 SSE") {
        Button(onClick = { onIntent(ComposeTestIntent.SendSseMessage) }) { Text("发送SSE消息") }
        Spacer(modifier = Modifier.height(8.dp))
        TestStateLine("状态: ", getSseStateText(state.sseState))
        TestStateLine("回复: ", state.sseMessage.ifBlank { "暂未收到消息" })
    }
}

private fun getRealtimeStateText(state: RealtimeChatState): String = when (state) {
    is RealtimeChatState.NotInitialized -> "未初始化"
    is RealtimeChatState.Initializing -> "正在初始化..."
    is RealtimeChatState.InitializedConnected -> "已初始化并且已经连接"
    is RealtimeChatState.RecordingAndSending -> "正在录音..."
    is RealtimeChatState.Receiving -> "正在接收..."
    is RealtimeChatState.Disconnected -> "已断开"
    is RealtimeChatState.Error -> "错误: ${state.message}"
    else -> "未知状态"
}

private fun getAudioStateText(state: AudioRecordPlayState): String = when (state) {
    is AudioRecordPlayState.NotInitialized -> "未初始化"
    is AudioRecordPlayState.Initializing -> "正在初始化..."
    is AudioRecordPlayState.Ready -> "就绪"
    is AudioRecordPlayState.Recording -> "正在录音..."
    is AudioRecordPlayState.RecordedAndPlayable -> "录音结束，可播放"
    is AudioRecordPlayState.Playing -> "正在播放..."
    is AudioRecordPlayState.PlayedEnd -> "播放结束"
    is AudioRecordPlayState.Error -> "错误: ${state.message}"
    else -> "未知状态"
}

private fun getWebsocketStateText(state: WebsocketState): String = when (state) {
    is WebsocketState.NotInitialized -> "未初始化"
    is WebsocketState.Initializing -> "初始化中..."
    is WebsocketState.InitializedNotConnected -> "未连接"
    is WebsocketState.Connected -> "已连接"
    is WebsocketState.Sending -> "正在发送消息..."
    is WebsocketState.Receiving -> "正在接收消息..."
    is WebsocketState.Disconnected -> "已断开连接"
    is WebsocketState.Error -> "错误: ${state.message}"
    else -> "未知状态"
}

private fun getTtsStateText(state: TtsChatState): String = when (state) {
    is TtsChatState.NotInitialized -> "未初始化"
    is TtsChatState.Initializing -> "初始化中..."
    is TtsChatState.InitializationFailed -> "初始化失败: ${state.message}"
    is TtsChatState.Idle -> "Android端就绪"
    is TtsChatState.Loading -> "连接中..."
    is TtsChatState.Streaming -> "接收中..."
    is TtsChatState.Success -> "对话完成"
    is TtsChatState.Error -> "错误: ${state.message}"
    else -> "未知状态"
}

private fun getSseStateText(state: ChatState): String = when (state) {
    is ChatState.Idle -> "Android端就绪"
    is ChatState.Loading -> "连接中..."
    is ChatState.Streaming -> "接收中..."
    is ChatState.Success -> "对话完成"
    is ChatState.Error -> "错误: ${state.message}"
    else -> "未知状态"
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun ComposeTestScreenPreview() {
    MagicVectorTheme {
        ComposeTestScreen(
            state = ComposeTestState(
                realtimeChat2State = RealtimeChatState.InitializedConnected,
                realtimeChat2Message = "你好，这里是RT2",
                websocketState = WebsocketState.Connected,
                ttsSseState = TtsChatState.Streaming,
                sseState = ChatState.Loading
            ),
            onIntent = {},
            withAudioPermission = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun RealtimeChat2SectionPreview() {
    MagicVectorTheme {
        RealtimeChat2Section(
            state = ComposeTestState(
                realtimeChat2State = RealtimeChatState.InitializedConnected,
                realtimeChat2Message = "测试消息",
                realtimeChat2Volume = 0.46f
            ),
            onIntent = {},
            withAudioPermission = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun WebsocketSectionPreview() {
    MagicVectorTheme {
        WebsocketSection(
            state = ComposeTestState(
                websocketState = WebsocketState.Connected,
                websocketMessageHistory = "msg1\nmsg2"
            ),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun AgentAudioSectionPreview() {
    MagicVectorTheme {
        AudioRecordSection(
            state = ComposeTestState(
                audioRecordPlayState = AudioRecordPlayState.Recording,
                audioRecordVolume = 0.73f
            ),
            onIntent = {},
            withAudioPermission = {}
        )
    }
}
