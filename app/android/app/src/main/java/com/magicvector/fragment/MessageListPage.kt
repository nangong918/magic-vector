package com.magicvector.fragment

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.core.baseutil.network.networkLoad.NetworkLoadUtils
import com.data.domain.ao.message.MessageContactItemAo
import com.data.domain.vo.message.MessageContactItemVo
import com.magicvector.viewModel.fragment.MessageListIntent
import com.magicvector.viewModel.fragment.MessageListMviVm
import com.magicvector.viewModel.fragment.MessageListState
import com.magicvector.ui.view.messageList.MessageListItem
import com.magicvector.viewModel.fragment.MessageListEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen(
    modifier: Modifier = Modifier,
    isServiceBound: Boolean,
    viewModel: MessageListMviVm = MessageListMviVm(),
    onCreateAgentClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
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
                is MessageListEffect.ShowLoadingDialog -> {
                    NetworkLoadUtils.showDialog(context)
                }
                is MessageListEffect.DismissLoadingDialog -> {
                    NetworkLoadUtils.dismissDialogSafety(context)
                }
                is MessageListEffect.LoadNetworkData -> {

                }
                // 其他 effect 由父页面处理
                else -> {}
            }
        }
    }

    // 下拉刷新
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.processIntent(MessageListIntent.Refresh) },
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.messages.isEmpty()) {
                // 空状态
                EmptyStateView()
            } else {
                // 消息列表
                MessageListContent(
                    state = state,
                    listState = listState,
                    onItemClick = { position ->
                        viewModel.processIntent(MessageListIntent.SelectMessage(position))
                    }
                )
            }

            // 创建Agent的FAB
            CreateAgentFloatingButton(
                onClick = onCreateAgentClick
            )
        }
    }
}

@Composable
fun MessageListContent(
    state: MessageListState,
    listState: LazyListState,
    onItemClick: (Int) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部"新消息"提示栏
//        item {
//            NewMessageHeader(
//                unreadCount = state.messageCount,
//                onClick = { /* 处理点击新消息提示 */ }
//            )
//        }

        // 消息列表
        items(
            items = state.messages,
            key = { it.contactId ?: it.hashCode().toString() }
        ) { message ->
            MessageListItem(
                avatarUrl = message.vo.avatarUrl,
                name = message.vo.name,
                messagePreview = message.vo.getMessagePreview(),
                time = message.vo.time ?: "",
                unreadCount = message.vo.unreadCount,
                onClick = {
                    val position = state.messages.indexOf(message)
                    if (position >= 0) {
                        onItemClick(position)
                    }
                }
            )
        }
    }
}

// 空状态组件
@Composable
fun EmptyStateView(
    modifier: Modifier = Modifier
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
                text = stringResource(id = com.view.appview.R.string.have_no_message_now),
                color = colorResource(id = com.view.appview.R.color.s1_800),
                fontSize = 20.sp
            )
        }
    }
}

// 创建Agent的FAB
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


@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    MaterialTheme {
        EmptyStateView()
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun MessageListWith10ItemsPreview() {
    MaterialTheme {
        // 创建模拟数据
        val mockState = createMockMessageListState(10)
        val listState = rememberLazyListState()

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 标题
            Text(
                text = "消息列表预览 (10条消息)",
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 消息列表内容
            MessageListContent(
                state = mockState,
                listState = listState,
                onItemClick = { position ->
                    println("点击了第 $position 条消息")
                }
            )
        }
    }
}


@Preview(showBackground = true, heightDp = 800)
@Composable
private fun MessageListWithMixedStatesPreview() {
    MaterialTheme {
        // 创建混合状态的模拟数据
        val mockState = createMixedStateMessageList()
        val listState = rememberLazyListState()

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "消息列表预览 (混合状态)",
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            MessageListContent(
                state = mockState,
                listState = listState,
                onItemClick = { position ->
                    println("点击了第 $position 条消息")
                }
            )
        }
    }
}

// 创建模拟的 MessageListState
private fun createMockMessageListState(count: Int): MessageListState {
    val messages = List(count) { index ->
        createMockMessageItem(index)
    }

    return MessageListState(
        isLoading = false,
        isRefreshing = false,
        messages = messages,
        messageCount = count,
        error = null,
        isFirstOpen = false,
        needLoadNetworkData = false
    )
}


// 创建混合状态的消息列表
private fun createMixedStateMessageList(): MessageListState {
    val messages = listOf(
        createMockMessageItem(0, "张三", "你好，最近怎么样？", "10:30", 0),
        createMockMessageItem(1, "李四", "文件已发送，请查收", "09:15", 2),
        createMockMessageItem(2, "系统通知", "你的Agent已创建成功", "昨天", 1),
        createMockMessageItem(3, "王五", "这是一条非常长的消息预览，用来测试换行效果，应该只显示一行并在末尾显示省略号", "2024-01-01", 3),
        createMockMessageItem(4, "赵六", "收到请回复", "昨天", 5),
        createMockMessageItem(5, "钱七", "周末一起去吃饭吗？", "11:20", 0),
        createMockMessageItem(6, "孙八", "项目进展怎么样了？", "周三", 1),
        createMockMessageItem(7, "周九", "图片已上传", "周二", 0),
        createMockMessageItem(8, "吴十", "好的，我知道了", "周一", 0),
        createMockMessageItem(9, "郑十一", "这是一个超长用户名的用户用来测试显示效果", "2024-01-02", 99)
    )

    return MessageListState(
        isLoading = false,
        isRefreshing = false,
        messages = messages,
        messageCount = messages.size,
        error = null,
        isFirstOpen = false,
        needLoadNetworkData = false
    )
}

// 创建模拟的 MessageContactItemAo
private fun createMockMessageItem(
    index: Int,
    name: String = "用户 $index",
    preview: String = "这是第 $index 条消息的预览内容",
    time: String = when (index % 3) {
        0 -> "10:30"
        1 -> "昨天"
        else -> "2024-01-${index + 1}"
    },
    unreadCount: Int = when (index % 4) {
        0 -> 0
        1 -> 1
        2 -> 3
        else -> 5
    }
): MessageContactItemAo {
    return MessageContactItemAo().apply {
        contactId = "contact_$index"
        timestamp = System.currentTimeMillis() - index * 3_600_000L

        vo = MessageContactItemVo().apply {
            this.avatarUrl = null  // 使用默认头像
            this.name = name
            this.time = time
            this.unreadCount = unreadCount
        }
    }
}