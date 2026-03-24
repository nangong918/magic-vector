package com.magicvector.fragment

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.magicvector.ui.theme.Green10
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.ui.view.activity.bottomRoundedBackground
import com.magicvector.ui.view.chat.MessageListView
import com.magicvector.ui.view.chat.SendMessageView
import com.magicvector.ui.view.chat.rememberChatListState
import com.magicvector.ui.view.chat.rememberSendMessageState
import com.magicvector.viewModel.fragment.AgentTextChatFragmentEffect
import com.magicvector.viewModel.fragment.AgentTextChatFragmentIntent
import com.magicvector.viewModel.fragment.AgentTextChatFragmentVm

@Composable
fun AgentTextChatFragment(
    vm: AgentTextChatFragmentVm,
    onSendTextToAgent: (String) -> Unit,
    onSwitchToEmojiPage: () -> Unit,
    onAudioTouch: (Boolean) -> Unit
) {
    val uiState by vm.uiState.collectAsState()
    val chatState = rememberChatListState()
    val sendMessageState = rememberSendMessageState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.isEnableSend) {
        sendMessageState.isEnableSend = uiState.isEnableSend
    }

    LaunchedEffect(uiState.messages) {
        chatState.replaceMessages(uiState.messages)
        if (!uiState.messages.isEmpty()) {
            chatState.scrollToBottomImmediate(coroutineScope)
        }
    }

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                is AgentTextChatFragmentEffect.AppendMessage -> {
                    chatState.insertMessageAndScroll(effect.message, coroutineScope)
                }
                is AgentTextChatFragmentEffect.ForwardSendText -> {
                    onSendTextToAgent(effect.text)
                }
                AgentTextChatFragmentEffect.ForwardSwitchToEmojiPage -> {
                    onSwitchToEmojiPage()
                }
                AgentTextChatFragmentEffect.ForwardStartSendVoice -> onAudioTouch(true)
                AgentTextChatFragmentEffect.ForwardStopSendVoice -> onAudioTouch(false)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            SendMessageView(
                state = sendMessageState,
                onSendClick = { vm.processIntent(AgentTextChatFragmentIntent.UserSendText(it)) },
                onImageClick = {},
                onCallClick = { vm.processIntent(AgentTextChatFragmentIntent.SwitchToEmojiPage) },
                onVideoClick = { vm.processIntent(AgentTextChatFragmentIntent.SwitchToEmojiPage) },
                onAudioTouch = { vm.processIntent(AgentTextChatFragmentIntent.OnAudioTouch(it)) }
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
                MessageListView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 5.dp),
                    state = chatState
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 700)
@Composable
private fun AgentTextChatFragmentPreview() {
    MagicVectorTheme {
        val vm = AgentTextChatFragmentVm()
        AgentTextChatFragment(
            vm = vm,
            onSendTextToAgent = {},
            onSwitchToEmojiPage = {},
            onAudioTouch = {}
        )
    }
}
