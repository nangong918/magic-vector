package com.magicvector.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.magicvector.ui.theme.*
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.ui.view.activity.ChatToolbar
import com.magicvector.ui.view.activity.MessageList
import com.magicvector.ui.view.activity.SendMessagePlaceholder
import com.magicvector.ui.view.activity.bottomRoundedBackground
import com.magicvector.ui.view.chat.rememberChatListState

class ComposeChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MagicVectorTheme {
                ChatScreen(onBackClick = {})
            }
        }
    }
}

@Composable
private fun ChatScreen(
    agentName: String = "",
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    // 将 ChatListState 提升到 ListScreen 级别
    val chatState = rememberChatListState()
    val coroutineScope = rememberCoroutineScope()


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ChatToolbar (
                title = agentName,
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            // 传递 chatState 给发送消息组件
            SendMessagePlaceholder(chatState = chatState, coroutineScope = coroutineScope)
        }
    ) { innerPadding ->
        Column (
            modifier = Modifier.padding(innerPadding)
                .fillMaxSize()
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

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ChatScreenPreview() {
    MagicVectorTheme {
        ChatScreen(
            agentName = "鸦羽天下第一!!",
            onBackClick = {}
        )
    }
}