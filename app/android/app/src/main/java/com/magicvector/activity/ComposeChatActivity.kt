package com.magicvector.activity

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.core.baseutil.permissions.GainPermissionCallback
import com.core.baseutil.ui.ToastUtils
import com.magicvector.ui.theme.*
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.ui.view.activity.ChatToolbar
import com.magicvector.ui.view.activity.MessageList
import com.magicvector.ui.view.activity.bottomRoundedBackground
import com.magicvector.ui.view.call.CallDialogView
import com.magicvector.ui.view.chat.MessageItem
import com.magicvector.ui.view.chat.SendMessageView
import com.magicvector.ui.view.chat.rememberSendMessageState
import com.magicvector.ui.view.chat.rememberChatListState
import com.magicvector.utils.permissions.ComposePermissionUtils
import com.magicvector.viewModel.activity.ChatEffect
import com.magicvector.viewModel.activity.ChatIntent
import com.magicvector.viewModel.activity.ComposeChatVm
import java.text.SimpleDateFormat
import java.util.Date

class ComposeChatActivity : FragmentActivity() {
    private val vm: ComposeChatVm by viewModels()
    private val callPermissionUtils = ComposePermissionUtils()
    private val videoPermissionUtils = ComposePermissionUtils()

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        callPermissionUtils.registerPermissionLauncher(
            activity = this,
            mustPermissions = arrayOf(Manifest.permission.RECORD_AUDIO)
        )
        videoPermissionUtils.registerPermissionLauncher(
            activity = this,
            mustPermissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA),
            optionalPermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        )
        enableEdgeToEdge()
        setContent {
            MagicVectorTheme {
                ChatScreen(
                    vm = vm,
                    onBackClick = { finish() },
                    callPermissionUtils = callPermissionUtils,
                    videoPermissionUtils = videoPermissionUtils
                )
            }
        }
        vm.processIntent(ChatIntent.Initialize(intent, this))
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun onResume() {
        super.onResume()
        vm.processIntent(ChatIntent.Resume)
    }
}

@Composable
private fun ChatScreen(
    vm: ComposeChatVm,
    onBackClick: () -> Unit,
    callPermissionUtils: ComposePermissionUtils,
    videoPermissionUtils: ComposePermissionUtils,
) {
    val context = LocalContext.current
    val uiState by vm.uiState.collectAsState()
    val chatState = rememberChatListState()
    val sendMessageState = rememberSendMessageState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.isEnableSend) {
        sendMessageState.isEnableSend = uiState.isEnableSend
    }

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                ChatEffect.Finish -> {
                    (context as? FragmentActivity)?.finish()
                }
                ChatEffect.RequestCallPermission -> {
                    val activity = context as? FragmentActivity ?: return@collect
                    callPermissionUtils.requestPermissions(activity, object : GainPermissionCallback {
                        override fun allGranted() {
                            vm.processIntent(ChatIntent.CallPermissionGranted(activity))
                        }

                        override fun notGranted(notGrantedPermissions: Array<String?>?) {
                            vm.processIntent(ChatIntent.CallPermissionDenied)
                        }

                        override fun always() {
                        }
                    })
                }
                ChatEffect.RequestVideoPermission -> {
                    val activity = context as? FragmentActivity ?: return@collect
                    videoPermissionUtils.requestPermissions(activity, object : GainPermissionCallback {
                        override fun allGranted() {
                            vm.processIntent(ChatIntent.VideoPermissionGranted)
                        }

                        override fun notGranted(notGrantedPermissions: Array<String?>?) {
                            vm.processIntent(ChatIntent.VideoPermissionDenied)
                        }

                        override fun always() {
                        }
                    })
                }
                is ChatEffect.NavigateToVideoCall -> {
                    val activity = context as? FragmentActivity ?: return@collect
                    val goIntent = Intent(activity, ComposeAgentEmojiActivity::class.java).apply {
                        putExtra("agentId", effect.agentId)
                        putExtra("agentName", effect.agentName)
                    }
                    activity.startActivity(goIntent)
                }
                is ChatEffect.ShowToast -> {
                    val activity = context as? FragmentActivity ?: return@collect
                    ToastUtils.showToastActivity(activity, effect.message)
                }
                is ChatEffect.ShowToastRes -> {
                    val activity = context as? FragmentActivity ?: return@collect
                    ToastUtils.showToastActivity(activity, activity.getString(effect.messageRes))
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ChatToolbar (
                title = uiState.title,
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            SendMessageView(
                state = sendMessageState,
                onSendClick = { message ->
                    val sentMessage = MessageItem.Sent(
                        id = System.currentTimeMillis().toString(),
                        messageText = message,
                        timeText = SimpleDateFormat("yyyy/MM/dd HH:mm").format(Date())
                    )
                    chatState.insertMessageAndScroll(sentMessage, coroutineScope)
                    vm.processIntent(ChatIntent.SendTextMessage(message))
                },
                onImageClick = {},
                onCallClick = { vm.processIntent(ChatIntent.RequestCall) },
                onVideoClick = { vm.processIntent(ChatIntent.RequestVideoCall) },
                onAudioTouch = { isStart ->
                    if (isStart) {
                        vm.processIntent(ChatIntent.StartSendVoice(coroutineScope))
                    } else {
                        vm.processIntent(ChatIntent.StopSendVoice)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column (
            modifier = Modifier.padding(innerPadding)
                .fillMaxSize()
        ) {
            // 背景视图 (对应 viewBackground)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .bottomRoundedBackground(color = Green10, cornerRadius = 32.dp)
            ) {
                MessageList(chatState = chatState)
            }
        }

        CallDialogView(
            visible = uiState.isCallDialogVisible,
            agentName = uiState.title,
            agentAvatar = uiState.avatarUrl,
            chatState = uiState.callVadState,
            chatMessage = uiState.callMessage,
            isMicClosed = uiState.isMicClosed,
            onCloseClick = { vm.processIntent(ChatIntent.EndCall) },
            onMicClick = { vm.processIntent(ChatIntent.ToggleCallMute) },
            onCallEndClick = { vm.processIntent(ChatIntent.EndCall) }
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ChatScreenPreview() {
    MagicVectorTheme {
        ChatPreviewContent()
    }
}

@Composable
private fun ChatPreviewContent() {
    val chatState = rememberChatListState()
    val sendMessageState = rememberSendMessageState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ChatToolbar(
                title = "预览Agent",
                onBackClick = {}
            )
        },
        bottomBar = {
            SendMessageView(
                state = sendMessageState,
                onSendClick = { message ->
                    val sentMessage = MessageItem.Sent(
                        id = System.currentTimeMillis().toString(),
                        messageText = message,
                        timeText = SimpleDateFormat("yyyy/MM/dd HH:mm").format(Date())
                    )
                    chatState.insertMessageAndScroll(sentMessage, coroutineScope)
                },
                onImageClick = {},
                onCallClick = {},
                onVideoClick = {},
                onAudioTouch = {}
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .bottomRoundedBackground(color = Green10, cornerRadius = 32.dp)
            ) {
                MessageList(chatState = chatState)
            }
        }
    }
}