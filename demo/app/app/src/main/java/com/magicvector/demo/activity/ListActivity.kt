package com.magicvector.demo.activity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import com.magicvector.demo.activity.ui.theme.*
import com.magicvector.demo.activity.ui.theme.AppDemoTheme
import com.magicvector.demo.R
import com.magicvector.demo.activity.ui.shape.bottomRoundedBackground
import com.magicvector.demo.view.message.ChatListState
import com.magicvector.demo.view.message.MessageItem
import com.magicvector.demo.view.message.MessageListView
import com.magicvector.demo.view.message.SendMessageView
import com.magicvector.demo.view.message.rememberChatListState
import kotlinx.coroutines.CoroutineScope

class ListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppDemoTheme {
                ListScreen()
            }
        }
    }
}


@Composable
fun ListScreen() {
    val activity = LocalActivity.current
    // 将 ChatListState 提升到 ListScreen 级别
    val chatState = rememberChatListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BasicToolbar(
                title = "鸦羽天下第一!",
                onBackClick = {
                    activity?.finish()
                }
            )
        },
        bottomBar = {
            // 传递 chatState 给发送消息组件
            SendMessagePlaceholder(chatState = chatState, coroutineScope = coroutineScope)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(color = A1_200)
        ) {
            // 背景视图 (对应 viewBackground)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .bottomRoundedBackground(color = Green10, cornerRadius = 32.dp)
            ) {
                // 消息列表 (对应 rclv_message)
                MessageList(chatState = chatState)
            }
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
                MessageItem.Received(
                    id = "received_$index",
                    messageText = "这是收到的消息 $index",
                    chatTime = "2025/10/9 ${10 + index % 10}:${index % 60}",
                    isShowImage = index % 5 == 0
                )
            } else {
                MessageItem.Sent(
                    id = "sent_$index",
                    messageText = "这是发送的消息 $index",
                    timeText = "2025/10/9 ${10 + index % 10}:${index % 60}",
                    isShowImage = index % 5 == 0
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
                        MessageItem.Received(
                            id = "received_more_$newIndex",
                            messageText = "加载的历史消息 $newIndex",
                            chatTime = "2025/10/8 ${10 + newIndex % 10}:${newIndex % 60}",
                            isShowImage = newIndex % 5 == 0
                        )
                    } else {
                        MessageItem.Sent(
                            id = "sent_more_$newIndex",
                            messageText = "加载的历史消息 $newIndex",
                            timeText = "2025/10/8 ${10 + newIndex % 10}:${newIndex % 60}",
                            isShowImage = newIndex % 5 == 0
                        )
                    }
                }
                chatState.loadMoreMessages(moreMessages)
            },
            onMessageClick = { message ->
                // 处理消息点击事件
                println("点击了消息: ${when (message) {
                    is MessageItem.Received -> "收到: ${message.messageText}"
                    is MessageItem.Sent -> "发送: ${message.messageText}"
                }}")
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
            val newMessage = MessageItem.Sent(
                id = System.currentTimeMillis().toString(),
                messageText = message,
                timeText = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm").format(java.util.Date())
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

@Composable
fun BasicToolbar(
    title: String,
    onBackClick: () -> Unit,
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(color = A1_200)
    ) {
        val (backIcon, titleText) = createRefs()

        // 返回按钮
        Icon(
            painter = painterResource(id = R.drawable.chevron_left_24px),
            contentDescription = "返回",
            modifier = Modifier
                .size(24.dp)
                .constrainAs(backIcon) {
                    start.linkTo(parent.start, margin = 20.dp)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
                .clickable { onBackClick() },
            tint = S1_800
        )

        // 标题 - 类似 XML 的约束关系
        Text(
            text = title,
            color = S1_800,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .width(250.dp)
                .constrainAs(titleText) {
                    start.linkTo(backIcon.end, margin = 20.dp)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun ListPreview() {
    AppDemoTheme {
        ListScreen()
    }
}