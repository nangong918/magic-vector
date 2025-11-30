package com.magicvector.demo.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.magicvector.demo.activity.ui.theme.AppDemoTheme
import com.magicvector.demo.view.jetMessage.UserInput
import kotlinx.coroutines.launch

class ComposeListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppDemoTheme {
                ComposeListScreen()
            }
        }
    }
}

@Composable
fun ComposeListScreen() {

    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // 在 bottomBar 里添加 UserInput
            UserInput(
                onMessageSent = { message ->
                    // 处理消息发送逻辑
                },
                resetScroll = {
                    scope.launch {
                        scrollState.scrollToItem(0)
                    }
                }
            )
        }
    ) { innerPadding ->
        // 其他内容可以放在这里
        // 例如聊天记录的列表
        Column(modifier = Modifier.padding(innerPadding)) {
            // 此处可以放置聊天记录
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun GreetingPreview() {
    AppDemoTheme {
        ComposeListScreen()
    }
}