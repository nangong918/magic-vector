package com.magicvector.ui.view.chat

import android.annotation.SuppressLint
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import com.magicvector.domain.constant.chat.MessageTypeEnum
import com.magicvector.domain.constant.chat.RoleTypeEnum
import com.magicvector.domain.model.chat.ChatMessageModel
import com.magicvector.domain.vo.message.ChatBriefMessageVO
import com.magicvector.domain.vo.message.ChatMessageVO
import com.magicvector.ui.theme.MagicVectorTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// 聊天列表状态管理
class ChatListState {
    val messages = mutableStateListOf<ChatMessageModel>()
    var isLoading by mutableStateOf(false)
    var canLoadMore by mutableStateOf(true)
    var listState: LazyListState? = null

    // 滚动到底部
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

    // 添加新消息到尾部（按时间正序）
    fun addNewMessages(newMessages: List<ChatMessageModel>) {
        messages.addAll(newMessages.sortedBy { it.timestamp })
    }

    // 加载更多历史消息到头部
    fun loadMoreMessages(historyMessages: List<ChatMessageModel>) {
        messages.addAll(0, historyMessages.sortedBy { it.timestamp })
        canLoadMore = historyMessages.isNotEmpty()
    }

    // 插入单条消息到尾部
    fun insertMessage(message: ChatMessageModel) {
        messages.add(message)
    }

    // 插入消息并滚动到底部
    fun insertMessageAndScroll(message: ChatMessageModel, coroutineScope: CoroutineScope) {
        insertMessage(message)
        coroutineScope.launch {
            listState?.animateScrollToItem(messages.size - 1)
        }
    }

    // 替换所有消息
    fun replaceMessages(newMessages: List<ChatMessageModel>) {
        messages.clear()
        messages.addAll(newMessages.sortedBy { it.timestamp })
    }

    // 更新单条消息
    fun updateMessage(messageId: Long, update: (ChatMessageModel) -> ChatMessageModel) {
        val index = messages.indexOfFirst { it.messageId == messageId }
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
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (state.messages.isNotEmpty()) state.messages.size - 1 else 0
    )

    LaunchedEffect(listState) {
        state.listState = listState
    }

    LazyColumn(
        modifier = modifier,
        state = listState
    ) {
        items(
            count = state.messages.size,
            key = { index ->
                val message = state.messages[index]
                "${message.chatMessageVo
                    .briefMessageVo.role}_${message.messageId}"
            }
        ) { index ->
            val message = state.messages[index]

            when (message.chatMessageVo.briefMessageVo.role) {
                RoleTypeEnum.AGENT.value -> {
                    ReceivedMessage(
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .padding(vertical = 4.dp),
                        avatarUrl = null, // 可以从 Agent 信息获取
                        messageText = message.chatMessageVo.briefMessageVo.content,
                        messageImageUrl = message.chatMessageVo.imgUrl.takeIf { it.isNotEmpty() },
                        timeText = message.chatMessageVo.briefMessageVo.chatTime,
                        isShowImage = message.chatMessageVo.messageType != MessageTypeEnum.TEXT.value,
                    )
                }
                else -> {
                    SentMessage(
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .padding(vertical = 4.dp),
                        messageText = message.chatMessageVo.briefMessageVo.content,
                        messageImageUrl = message.chatMessageVo.imgUrl.takeIf { it.isNotEmpty() },
                        timeText = message.chatMessageVo.briefMessageVo.chatTime,
                        isShowImage = message.chatMessageVo.messageType != MessageTypeEnum.TEXT.value,
                    )
                }
            }
        }

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
private fun ChatScreen() {
    val chatState = rememberChatListState()

    LaunchedEffect(Unit) {
        val initialMessages = listOf(
            ChatMessageModel(
                chatMessageVo = ChatMessageVO(
                    briefMessageVo = ChatBriefMessageVO(
                        content = "你好！",
                        chatTime = "2025/10/9 10:00",
                        role = RoleTypeEnum.AGENT.value
                    ),
                    imgUrl = "",
                    messageType = MessageTypeEnum.TEXT.value
                ),
                agentId = 1L,
                userId = 1L,
                messageId = 1L,
                timestamp = 1696838400000L
            ),
            ChatMessageModel(
                chatMessageVo = ChatMessageVO(
                    briefMessageVo = ChatBriefMessageVO(
                        content = "你好！最近怎么样？",
                        chatTime = "2025/10/9 10:01",
                        role = RoleTypeEnum.USER.value
                    ),
                    imgUrl = "",
                    messageType = MessageTypeEnum.TEXT.value
                ),
                agentId = 1L,
                userId = 1L,
                messageId = 2L,
                timestamp = 1696838460000L
            ),
            ChatMessageModel(
                chatMessageVo = ChatMessageVO(
                    briefMessageVo = ChatBriefMessageVO(
                        content = "还不错，你呢？",
                        chatTime = "2025/10/9 10:02",
                        role = RoleTypeEnum.AGENT.value
                    ),
                    imgUrl = "",
                    messageType = MessageTypeEnum.TEXT.value
                ),
                agentId = 1L,
                userId = 1L,
                messageId = 3L,
                timestamp = 1696838520000L
            )
        )
        chatState.replaceMessages(initialMessages)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MessageListView(
            modifier = Modifier.weight(1f),
            state = chatState,
            onLoadMore = {
                // 加载更多历史消息
                val historyMessages = listOf(
                    ChatMessageModel(
                        chatMessageVo = ChatMessageVO(
                            briefMessageVo = ChatBriefMessageVO(
                                content = "这是历史消息",
                                chatTime = "2025/10/8 09:00",
                                role = RoleTypeEnum.AGENT.value
                            ),
                            imgUrl = "",
                            messageType = MessageTypeEnum.TEXT.value
                        ),
                        agentId = 1L,
                        userId = 1L,
                        messageId = 0L,
                        timestamp = 1696752000000L
                    )
                )
                chatState.loadMoreMessages(historyMessages)
            }
        )
    }
}

// 预览
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun MessageListViewPreview() {
    MagicVectorTheme {
        ChatScreen()
    }
}