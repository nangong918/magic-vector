package com.magicvector.fragment

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.magicvector.domain.bo.AgentChatBO
import com.magicvector.domain.model.agent.AgentChatModel
import com.magicvector.domain.vo.agent.AgentChatVO
import com.magicvector.domain.vo.agent.AgentVO
import com.magicvector.domain.vo.message.ChatBriefMessageVO
import com.magicvector.ui.view.messageList.MessageListItem
import com.magicvector.ui.view.NetworkLoadingOverlay
import com.magicvector.viewModel.fragment.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen(
    modifier: Modifier = Modifier,
    viewModel: MessageListMviVm,
    onCreateAgentClick: () -> Unit = {},
    onOpenChat: (AgentChatBO) -> Unit = {},
    onOpenAgentEditor: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val dataState by viewModel.dataState.collectAsState()
    val agents by viewModel.agents.collectAsState()  // 直接从 Manager 观察数据
    val listState = rememberLazyListState()

    // 初始化
    LaunchedEffect(Unit) {
        viewModel.processIntent(MessageListIntent.Initialize)
    }

    // 观察 Effect
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MessageListEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is MessageListEffect.NavigateToChat -> {
                    onOpenChat(effect.bo)
                }
                is MessageListEffect.OpenAgentEditor -> {
                    onOpenAgentEditor(effect.agentId)
                }
                else -> {}
            }
        }
    }

    // 下拉刷新
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.processIntent(MessageListIntent.Refresh) },
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                MessageServiceStatusView(
                    state = dataState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (uiState.uiMode) {
                        MessageListUiMode.NO_AGENT -> {
                            EmptyStateView(
                                onCreateAgentClick = onCreateAgentClick
                            )
                        }
                        MessageListUiMode.HAS_AGENT -> {
                            AgentListContent(
                                agents = agents,
                                listState = listState,
                                onItemClick = { position ->
                                    viewModel.processIntent(MessageListIntent.SelectAgent(position))
                                },
                                onItemLongClick = { position ->
                                    viewModel.processIntent(MessageListIntent.EditAgent(position))
                                }
                            )
                        }
                    }
                }
            }

            if (uiState.uiMode != MessageListUiMode.NO_AGENT) {
                CreateAgentFloatingButton(
                    onClick = onCreateAgentClick,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }

            // 顶层加载遮罩
            NetworkLoadingOverlay(isLoading = uiState.isLoading)
        }
    }
}

@Composable
private fun MessageServiceStatusView(
    state: MessageListDataState,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusText) = when {
        state.hasException -> Color(0xFFD32F2F) to "异常"
        !state.isNetworkOnline -> Color(0xFF9E9E9E) to "当前离线"
        !state.isServiceBound -> Color(0xFF9E9E9E) to "未绑定service"
        !state.isWsConnected -> Color(0xFF9E9E9E) to "未连接ws"
        else -> Color(0xFF2E7D32) to "已绑定service并连接ws"
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = statusText, color = statusColor, fontSize = 14.sp)
    }
}

@Composable
fun AgentListContent(
    agents: List<AgentChatModel>,
    listState: LazyListState,
    onItemClick: (Int) -> Unit,
    onItemLongClick: (Int) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = agents,
            key = { it.agentId }
        ) { agent ->
            // 这里需要根据 AgentChatModel 转换为 MessageListItem 所需的数据
            // 注意：Agent 列表显示的是 Agent 的基本信息，不是消息预览
            // 你需要根据实际需求调整显示内容
            MessageListItem(
                avatarUrl = agent.agentChatVo?.agentVo?.avatarUrl,
                name = agent.agentChatVo?.agentVo?.name ?: "未知",
                messagePreview = agent.agentChatVo?.chatBriefMessageVo?.content ?: "暂无消息",
                time = agent.agentChatVo?.chatBriefMessageVo?.chatTime ?: "",
                unreadCount = agent.agentChatVo?.unreadCount ?: 0,
                onClick = {
                    val position = agents.indexOf(agent)
                    if (position >= 0) {
                        onItemClick(position)
                    }
                },
                onLongClick = {
                    val position = agents.indexOf(agent)
                    if (position >= 0) {
                        onItemLongClick(position)
                    }
                }
            )
        }
    }
}

// 空状态组件
@SuppressLint("ResourceType")
@Composable
fun EmptyStateView(
    modifier: Modifier = Modifier,
    onCreateAgentClick: () -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = com.view.appview.R.xml.raven_24px),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = colorResource(id = com.view.appview.R.color.s1_800)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "当前还没有Agent",
                color = colorResource(id = com.view.appview.R.color.s1_800),
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onCreateAgentClick) {
                Text(text = stringResource(id = com.view.appview.R.string.create_agent))
            }
        }
    }
}

// 创建Agent的FAB
@SuppressLint("ResourceType")
@Composable
fun CreateAgentFloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(55.dp)
            .padding(bottom = 80.dp, end = 24.dp),
        containerColor = colorResource(id = com.view.appview.R.color.s1_200),
        contentColor = colorResource(id = com.view.appview.R.color.s1_800)
    ) {
        Icon(
            painter = painterResource(id = com.view.appview.R.xml.add_24px),
            contentDescription = stringResource(id = com.view.appview.R.string.create_agent)
        )
    }
}

// ========== Preview ==========

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    MaterialTheme {
        EmptyStateView(onCreateAgentClick = {})
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AgentListPreview() {
    MaterialTheme {
        val mockAgents = createMockAgents(10)
        val listState = rememberLazyListState()

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "Agent列表预览 (10个Agent)",
                modifier = Modifier.padding(16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            AgentListContent(
                agents = mockAgents,
                listState = listState,
                onItemClick = { position ->
                    println("点击了第 $position 个Agent")
                },
                onItemLongClick = { position ->
                    println("长按了第 $position 个Agent")
                }
            )
        }
    }
}

// 创建模拟的 AgentChatModel 列表
private fun createMockAgents(count: Int): List<AgentChatModel> {
    return List(count) { index ->
        AgentChatModel().apply {
            agentId = index.toLong()
            userId = 1L
            lastChatTime = System.currentTimeMillis() - index * 3_600_000L
            updatedAt = System.currentTimeMillis() - index * 3_600_000L

            agentChatVo = AgentChatVO(
                unreadCount = (index % 5),
                agentVo = AgentVO(
                    name = "Agent $index",
                    description = "这是Agent $index 的描述",
                    avatarUrl = null
                ),
                chatBriefMessageVo = ChatBriefMessageVO(
                    content = "这是第 $index 条消息的预览内容",
                    chatTime = when (index % 3) {
                        0 -> "10:30"
                        1 -> "昨天"
                        else -> "2024-01-${index + 1}"
                    },
                    role = 0
                )
            )
        }
    }
}