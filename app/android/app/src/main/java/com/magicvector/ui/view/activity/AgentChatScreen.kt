package com.magicvector.ui.view.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.magicvector.fragment.AgentEmojiFragment
import com.magicvector.fragment.AgentTextChatFragment
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.ui.view.NetworkLoadingOverlay
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

    Box(modifier = Modifier.fillMaxSize()) {
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
        NetworkLoadingOverlay(isLoading = uiState.isLoading)
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

@Preview(showBackground = true, heightDp = 800)
@Composable
fun ComposeAgentChatScreenPreview() {
    MagicVectorTheme {
        // 创建模拟的 ViewModel
        val emojiVm = AgentEmojiFragmentVm()
        val textVm = AgentTextChatFragmentVm()

        // 模拟 UI 状态
        val uiState = AgentChatUiState(
            title = "智能助手",
            currentPage = 0
        )

        ComposeAgentChatScreen(
            uiState = uiState,
            emojiVm = emojiVm,
            textVm = textVm,
            onBackClick = {},
            onPageChanged = {},
            onToggleMic = {},
            onRequestWakeUp = {},
            onEndVoiceMode = {},
            onSendTextToAgent = {},
            onSwitchToEmojiPage = {},
            onAudioTouch = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
fun ComposeAgentChatScreenSecondPagePreview() {
    MagicVectorTheme {
        val emojiVm = AgentEmojiFragmentVm()
        val textVm = AgentTextChatFragmentVm()

        val uiState = AgentChatUiState(
            title = "智能助手",
            currentPage = 1  // 第二页
        )

        ComposeAgentChatScreen(
            uiState = uiState,
            emojiVm = emojiVm,
            textVm = textVm,
            onBackClick = {},
            onPageChanged = {},
            onToggleMic = {},
            onRequestWakeUp = {},
            onEndVoiceMode = {},
            onSendTextToAgent = {},
            onSwitchToEmojiPage = {},
            onAudioTouch = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PagerDotsPreview() {
    MagicVectorTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 第一页选中
            PagerDots(currentPage = 0)

            Spacer(modifier = Modifier.height(20.dp))

            // 第二页选中
            PagerDots(currentPage = 1)

            Spacer(modifier = Modifier.height(20.dp))

            // 交互式预览
            InteractivePagerDotsPreview()
        }
    }
}

@Composable
private fun InteractivePagerDotsPreview() {
    var currentPage by remember { mutableStateOf(0) }

    Column {
        Text("点击切换页面")
        PagerDots(currentPage = currentPage)

        Row(
            modifier = Modifier.padding(top = 8.dp)
        ) {
            androidx.compose.material3.Button(
                onClick = { currentPage = 0 },
                modifier = Modifier.weight(1f)
            ) {
                Text("第一页")
            }
            androidx.compose.material3.Button(
                onClick = { currentPage = 1 },
                modifier = Modifier.weight(1f)
            ) {
                Text("第二页")
            }
        }
    }
}
