package com.magicvector.demo.view.message

import android.annotation.SuppressLint
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment


import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.magicvector.demo.activity.ui.theme.AppDemoTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// 消息数据类
sealed class MessageItem {
    data class Received(
        val id: String,
        val avatarUrl: String? = null,
        val messageText: String,
        val messageImageUrl: String? = null,
        val chatTime: String,
        val isShowImage: Boolean = false
    ) : MessageItem()

    data class Sent(
        val id: String,
        val messageText: String,
        val messageImageUrl: String? = null,
        val timeText: String,
        val isShowImage: Boolean = false
    ) : MessageItem()
}

// 聊天列表状态管理
class ChatListState {
    val messages = mutableStateListOf<MessageItem>()
    var isLoading by mutableStateOf(false)
    var canLoadMore by mutableStateOf(true)
    var listState: LazyListState? = null

    // 滚动到底部 - 使用消息数量快照避免并发问题
    fun scrollToBottom(coroutineScope: CoroutineScope) {
        val state = listState ?: return

        coroutineScope.launch {
            val itemCount = messages.size
                if (itemCount > 0 && itemCount - 1 < state.layoutInfo.totalItemsCount) {
                state.animateScrollToItem(itemCount - 1)
            }
        }
    }

    // 立即滚动到底部（无动画）
    fun scrollToBottomImmediate(coroutineScope: CoroutineScope) {
        val state = listState ?: return

        coroutineScope.launch {
            val itemCount = messages.size
            if (itemCount > 0 && itemCount - 1 < state.layoutInfo.totalItemsCount) {
                state.scrollToItem(itemCount - 1)
            }
        }
    }

    // 修改：新消息添加到尾部（时间正序）
    fun addNewMessages(newMessages: List<MessageItem>) {
        messages.addAll(newMessages.sortedBy { // 改为正序排序
            when (it) {
                is MessageItem.Received -> it.chatTime
                is MessageItem.Sent -> it.timeText
            }
        })
    }

    // 修改：加载更多历史消息到头部
    fun loadMoreMessages(historyMessages: List<MessageItem>) {
        messages.addAll(0, historyMessages.sortedBy { // 添加到头部，正序排序
            when (it) {
                is MessageItem.Received -> it.chatTime
                is MessageItem.Sent -> it.timeText
            }
        })
        canLoadMore = historyMessages.isNotEmpty()
    }

    // 修改：插入单条消息到尾部（O(1) 操作）
    fun insertMessage(message: MessageItem) {
        messages.add(message) // 直接添加到尾部，性能最优
    }

    // 修改：插入消息并滚动到底部
    fun insertMessageAndScroll(message: MessageItem, coroutineScope: CoroutineScope) {
        // 先添加消息，再滚动
        insertMessage(message)
        // 使用新的大小滚动
        coroutineScope.launch {
            listState?.animateScrollToItem(messages.size - 1)
        }
    }

    // 更新单条消息
    fun updateMessage(messageId: String, update: (MessageItem) -> MessageItem) {
        val index = messages.indexOfFirst {
            when (it) {
                is MessageItem.Received -> it.id == messageId
                is MessageItem.Sent -> it.id == messageId
            }
        }
        if (index != -1) {
            messages[index] = update(messages[index])
        }
    }
}

@Composable
fun rememberChatListState(): ChatListState {
    return remember { ChatListState() }
}

@Composable
fun MessageListView(
    modifier: Modifier = Modifier,
    state: ChatListState = rememberChatListState(),
    onLoadMore: () -> Unit = { },
    onMessageClick: (MessageItem) -> Unit = { }
) {
    println("state.messages.size = ${state.messages.size}")
    // 关键：在创建 listState 时直接指定初始位置
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (state.messages.isNotEmpty()) state.messages.size - 1 else 0
    )

    // 保存 listState 到 ChatListState
    LaunchedEffect(listState) {
        state.listState = listState
    }

//    // 监听滚动到底部自动加载更多
//    val shouldLoadMore by remember(listState) {
//        derivedStateOf {
//            val layoutInfo = listState.layoutInfo
//            val totalItems = layoutInfo.totalItemsCount
//            if (totalItems == 0) return@derivedStateOf false
//
//            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
//            lastVisibleItem?.index == totalItems - 1 &&
//                    !state.isLoading &&
//                    state.canLoadMore
//        }
//    }
//
//    LaunchedEffect(shouldLoadMore) {
//        if (shouldLoadMore) {
//            state.isLoading = true
//            onLoadMore()
//            state.isLoading = false
//        }
//    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        // 取消新消息在底部，因为ArrayList插入到头后续的item都需要位移，单次时间复杂度O(n), 而新消息插入在最后一个是O(1)
//        reverseLayout = true
    ) {
        items(
            count = state.messages.size,
            key = { index ->
                when (val item = state.messages[index]) {
                    is MessageItem.Received -> "received_${item.id}"
                    is MessageItem.Sent -> "sent_${item.id}"
                }
            }
        ) { index ->
            val message = state.messages[index]

            when (message) {
                is MessageItem.Received -> {
                    ReceivedMessage(
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .padding(vertical = 4.dp),
                        avatarUrl = message.avatarUrl,
                        messageText = message.messageText,
                        messageImageUrl = message.messageImageUrl,
                        timeText = message.chatTime,
                        isShowImage = message.isShowImage
                    )
                }
                is MessageItem.Sent -> {
                    SentMessage(
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .padding(vertical = 4.dp),
                        messageText = message.messageText,
                        messageImageUrl = message.messageImageUrl,
                        timeText = message.timeText,
                        isShowImage = message.isShowImage
                    )
                }
            }
        }

        // 加载更多指示器
        if (state.isLoading) {
            item {
                LoadingIndicator()
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("加载中...")
    }
}

// 使用示例
@SuppressLint("SimpleDateFormat")
@Composable
fun ChatScreen() {
    val chatState = rememberChatListState()

    // 初始化示例数据
    LaunchedEffect(Unit) {
        val initialMessages = listOf(
            MessageItem.Received(
                id = "1",
                messageText = "你好！",
                chatTime = "2025/10/9 10:00"
            ),
            MessageItem.Sent(
                id = "2",
                messageText = "你好！最近怎么样？",
                timeText = "2025/10/9 10:01"
            ),
            MessageItem.Received(
                id = "3",
                messageText = "还不错，你呢？",
                chatTime = "2025/10/9 10:02"
            )
        )
        chatState.addNewMessages(initialMessages)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MessageListView(
            modifier = Modifier.weight(1f),
            state = chatState,
            onLoadMore = {
                // 加载更多历史消息
                val historyMessages = listOf(
                    MessageItem.Received(
                        id = "history_1",
                        messageText = "这是历史消息",
                        chatTime = "2025/10/8 09:00"
                    )
                )
                chatState.loadMoreMessages(historyMessages)
            },
            onMessageClick = { message ->
                // 处理消息点击
            }
        )
    }
}

// 预览
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun MessageListViewPreview() {
    AppDemoTheme {
        ChatScreen()
    }
}