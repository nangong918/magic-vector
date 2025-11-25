package com.magicvector.demo.view.message

import android.annotation.SuppressLint
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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

// 消息数据类
sealed class MessageItem {
    data class Received(
        val id: String,
        val avatarUrl: String? = null,
        val messageText: String,
        val messageImageUrl: String? = null,
        val timeText: String,
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

    // 添加新消息到顶部（时间倒序）
    fun addNewMessages(newMessages: List<MessageItem>) {
        messages.addAll(0, newMessages.sortedByDescending {
            when (it) {
                is MessageItem.Received -> it.timeText
                is MessageItem.Sent -> it.timeText
            }
        })
    }

    // 加载更多历史消息到底部
    fun loadMoreMessages(historyMessages: List<MessageItem>) {
        messages.addAll(historyMessages.sortedByDescending {
            when (it) {
                is MessageItem.Received -> it.timeText
                is MessageItem.Sent -> it.timeText
            }
        })
        canLoadMore = historyMessages.isNotEmpty()
    }

    // 插入单条消息（模拟发送新消息）
    fun insertMessage(message: MessageItem) {
        // 找到插入位置（按时间倒序）
        val index = messages.indexOfFirst {
            val currentTime = when (it) {
                is MessageItem.Received -> it.timeText
                is MessageItem.Sent -> it.timeText
            }
            val newTime = when (message) {
                is MessageItem.Received -> message.timeText
                is MessageItem.Sent -> message.timeText
            }
            currentTime < newTime
        }.takeIf { it != -1 } ?: messages.size

        messages.add(index, message)
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
    val listState = rememberLazyListState()

    // 监听滚动到顶部自动加载更多
    val shouldLoadMore by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 &&
                    !state.isLoading &&
                    state.canLoadMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            state.isLoading = true
            onLoadMore()
            state.isLoading = false
        }
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        reverseLayout = true // 时间倒序，新消息在底部
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
                        timeText = message.timeText,
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
                timeText = "2025/10/9 10:00"
            ),
            MessageItem.Sent(
                id = "2",
                messageText = "你好！最近怎么样？",
                timeText = "2025/10/9 10:01"
            ),
            MessageItem.Received(
                id = "3",
                messageText = "还不错，你呢？",
                timeText = "2025/10/9 10:02"
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
                        timeText = "2025/10/8 09:00"
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