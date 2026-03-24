package com.magicvector.ui.view.activity

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.magicvector.domain.constant.chat.RoleTypeEnum
import com.magicvector.domain.model.chat.ChatMessageModel
import com.magicvector.domain.vo.message.ChatBriefMessageVO
import com.magicvector.domain.vo.message.ChatMessageVO
import com.magicvector.ui.theme.*
import com.magicvector.ui.view.chat.ChatListState
import com.magicvector.ui.view.chat.MessageListView
import com.magicvector.ui.view.chat.SendMessageView
import kotlinx.coroutines.CoroutineScope


@Composable
fun ChatToolbar(
    title: String = "",
    onBackClick: () -> Unit = {}
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(colorResource(id = com.view.appview.R.color.a1_200))
    ) {
        val (imgvBack, tvTitle) = createRefs()

        // 返回按钮
        Image(
            painter = painterResource(id = com.view.appview.R.drawable.chevron_left_24px),
            contentDescription = "Back",
            modifier = Modifier
                .constrainAs(imgvBack) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
                .padding(start = 20.dp)
                .clickable(onClick = { onBackClick() }),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(colorResource(id = com.view.appview.R.color.s1_800))
        )

        // 标题文本
        Text(
            text = title,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .constrainAs(tvTitle) {
                    start.linkTo(imgvBack.end, margin = 20.dp)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    width = Dimension.value(250.dp)
                }
                .padding(4.dp),
            color = colorResource(id = com.view.appview.R.color.s1_800)
        )
    }
}

@Preview
@Composable
private fun ChatToolbarPreview() {
    ChatToolbar(title = "鸦羽天下第一!")
}

// 返回一个Modifier添加的额外函数
@SuppressLint("SuspiciousModifierThen")
@Composable
fun Modifier.bottomRoundedBackground(
    color: Color = A1_10,
    cornerRadius: Dp = 32.dp
): Modifier = this.then (
    clip(
        RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = cornerRadius,
            bottomEnd = cornerRadius
        )
    ).background(color)
)

@Preview(showBackground = true)
@Composable
fun BottomRoundedShapePreview() {
    MagicVectorTheme {
        Box(
            modifier = Modifier
                .size(200.dp, 100.dp)
                .bottomRoundedBackground(color = Green10)
        ) {
            Text("底部圆角")
        }
    }
}

// kotlin compose会执行 组合 和 重组，方法会被执行两次
@Composable
fun MessageList(chatState: ChatListState) {
    println("🎯 MessageList 被调用，messages.size = ${chatState.messages.size}")
    var isDataLoaded by remember { mutableStateOf(false) }

    // 初始化示例数据
    LaunchedEffect(Unit) {
        println("🚀 LaunchedEffect 开始执行")
        val initialMessages = List(20) { index ->
            if (index % 2 == 0) {
                // 接收的消息
                ChatMessageModel(
                    chatMessageVo = ChatMessageVO(
                        briefMessageVo = ChatBriefMessageVO(
                            content = "这是收到的消息 $index",
                            chatTime = "2025/10/9 ${10 + index % 10}:${index % 60}",
                            role = RoleTypeEnum.AGENT.value
                        ),
                        imgUrl = if (index % 5 == 0) "image_url" else "",
                        messageType = if (index % 5 == 0) com.magicvector.domain.constant.chat.MessageTypeEnum.IMAGE.value
                        else com.magicvector.domain.constant.chat.MessageTypeEnum.TEXT.value
                    ),
                    agentId = 1L,
                    userId = 1L,
                    messageId = index.toLong(),
                    timestamp = System.currentTimeMillis() - index * 60000L
                )
            } else {
                // 发送的消息
                ChatMessageModel(
                    chatMessageVo = ChatMessageVO(
                        briefMessageVo = ChatBriefMessageVO(
                            content = "这是发送的消息 $index",
                            chatTime = "2025/10/9 ${10 + index % 10}:${index % 60}",
                            role = RoleTypeEnum.USER.value
                        ),
                        imgUrl = if (index % 5 == 0) "image_url" else "",
                        messageType = if (index % 5 == 0) com.magicvector.domain.constant.chat.MessageTypeEnum.IMAGE.value
                        else com.magicvector.domain.constant.chat.MessageTypeEnum.TEXT.value
                    ),
                    agentId = 1L,
                    userId = 1L,
                    messageId = index.toLong(),
                    timestamp = System.currentTimeMillis() - index * 60000L
                )
            }
        }
        chatState.addNewMessages(initialMessages)
        isDataLoaded = true  // 数据加载完成
        println("✅ LaunchedEffect 执行完成，isDataLoaded = $isDataLoaded")
    }

    // 只有数据加载完成才显示列表
    if (isDataLoaded) {
        println("🎨 渲染 MessageListView 重组 (Recomposition)")
        MessageListView(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 5.dp),
            state = chatState,
            onLoadMore = {
                // 模拟加载更多数据
                val moreMessages = List(10) { index ->
                    val newIndex = chatState.messages.size + index
                    if (newIndex % 2 == 0) {
                        // 接收的历史消息
                        ChatMessageModel(
                            chatMessageVo = ChatMessageVO(
                                briefMessageVo = ChatBriefMessageVO(
                                    content = "加载的历史消息 $newIndex",
                                    chatTime = "2025/10/8 ${10 + newIndex % 10}:${newIndex % 60}",
                                    role = RoleTypeEnum.AGENT.value
                                ),
                                imgUrl = if (newIndex % 5 == 0) "image_url" else "",
                                messageType = if (newIndex % 5 == 0) com.magicvector.domain.constant.chat.MessageTypeEnum.IMAGE.value
                                else com.magicvector.domain.constant.chat.MessageTypeEnum.TEXT.value
                            ),
                            agentId = 1L,
                            userId = 1L,
                            messageId = newIndex.toLong(),
                            timestamp = System.currentTimeMillis() - newIndex * 60000L
                        )
                    } else {
                        // 发送的历史消息
                        ChatMessageModel(
                            chatMessageVo = ChatMessageVO(
                                briefMessageVo = ChatBriefMessageVO(
                                    content = "加载的历史消息 $newIndex",
                                    chatTime = "2025/10/8 ${10 + newIndex % 10}:${newIndex % 60}",
                                    role = RoleTypeEnum.USER.value
                                ),
                                imgUrl = if (newIndex % 5 == 0) "image_url" else "",
                                messageType = if (newIndex % 5 == 0) com.magicvector.domain.constant.chat.MessageTypeEnum.IMAGE.value
                                else com.magicvector.domain.constant.chat.MessageTypeEnum.TEXT.value
                            ),
                            agentId = 1L,
                            userId = 1L,
                            messageId = newIndex.toLong(),
                            timestamp = System.currentTimeMillis() - newIndex * 60000L
                        )
                    }
                }
                chatState.loadMoreMessages(moreMessages)
            }
        )
    }
    else {
        println("⏳ 显示加载指示器 初始组合 (Initial Composition)")
        // 数据加载中的占位符
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("加载中...")
        }
    }
}

@SuppressLint("SimpleDateFormat")
@Composable
fun SendMessagePlaceholder(chatState: ChatListState, coroutineScope: CoroutineScope) {
    SendMessageView(
        onSendClick = { message ->
            // 处理发送消息 - 插入到聊天列表
            val newMessage = ChatMessageModel(
                chatMessageVo = ChatMessageVO(
                    briefMessageVo = ChatBriefMessageVO(
                        content = message,
                        chatTime = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm").format(java.util.Date()),
                        role = RoleTypeEnum.USER.value
                    ),
                    imgUrl = "",
                    messageType = com.magicvector.domain.constant.chat.MessageTypeEnum.TEXT.value
                ),
                agentId = 0L,
                userId = 0L,
                messageId = System.currentTimeMillis(),
                timestamp = System.currentTimeMillis()
            )
            chatState.insertMessageAndScroll(
                message = newMessage,
                coroutineScope = coroutineScope
            )
        },
        onImageClick = {
            // 处理图片点击
            println("图片按钮点击")
        },
        onCallClick = {
            // 处理通话点击
            println("通话按钮点击")
        },
        onVideoClick = {
            // 处理视频点击
            println("视频按钮点击")
        },
        onAudioTouch = { isStart ->
            // 处理录音开始/结束
            if (isStart) {
                println("开始录音")
            } else {
                println("结束录音")
            }
        }
    )
}