package com.magicvector.ui.view.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.magicvector.fragment.AgentEmojiFragment
import com.magicvector.fragment.AgentTextChatFragment
import com.magicvector.viewModel.activity.AgentChatUiState
import com.magicvector.viewModel.fragment.AgentEmojiFragmentVm
import com.magicvector.viewModel.fragment.AgentTextChatFragmentVm

@Composable
fun ComposeAgentChatScreen(
    uiState: AgentChatUiState,
    emojiVm: AgentEmojiFragmentVm,
    textVm: AgentTextChatFragmentVm,
    onBackClick: () -> Unit,
    onPageChanged: (Int) -> Unit,
    onToggleMic: () -> Unit,
    onRequestWakeUp: () -> Unit,
    onEndVoiceMode: () -> Unit,
    onSendTextToAgent: (String) -> Unit,
    onSwitchToEmojiPage: () -> Unit,
    onAudioTouch: (Boolean) -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = uiState.currentPage,
        pageCount = { 2 }
    )

    LaunchedEffect(uiState.currentPage) {
        if (pagerState.currentPage != uiState.currentPage) {
            pagerState.animateScrollToPage(uiState.currentPage)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ChatToolbar(
                title = uiState.title,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            PagerDots(
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 8.dp)
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> AgentEmojiFragment(
                        vm = emojiVm,
                        onToggleMic = onToggleMic,
                        onRequestWakeUp = onRequestWakeUp,
                        onEndVoiceMode = onEndVoiceMode
                    )

                    else -> AgentTextChatFragment(
                        vm = textVm,
                        onSendTextToAgent = onSendTextToAgent,
                        onSwitchToEmojiPage = onSwitchToEmojiPage,
                        onAudioTouch = onAudioTouch
                    )
                }
            }
        }
    }
}

@Composable
private fun PagerDots(currentPage: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(2) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(if (selected) 10.dp else 8.dp)
                    .background(
                        color = if (selected) Color.Black else Color(0xFFBDBDBD),
                        shape = CircleShape
                    )
            )
        }
    }
}
