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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.magicvector.demo.activity.ui.theme.AppDemoTheme
import com.magicvector.demo.view.jetMessage.Message
import com.magicvector.demo.view.jetMessage.Messages
import com.magicvector.demo.view.jetMessage.UserInput
import com.magicvector.demo.view.jetMessage.data.exampleUiState
import kotlinx.coroutines.launch
import com.magicvector.demo.R
import java.text.SimpleDateFormat

class ComposeListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppDemoTheme {
                ComposeListScreen(navigateToProfile = {})
            }
        }
    }
}

private val uiState = exampleUiState

@Composable
fun ComposeListScreen(navigateToProfile: (String) -> Unit) {

    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val authorMe = stringResource(R.string.author_me)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // 在 bottomBar 里添加 UserInput
            UserInput(
                onMessageSent = { message ->
                    // 处理消息发送逻辑
                    val timeNowL = System.currentTimeMillis()
                    val timeNow = SimpleDateFormat("HH:mm:ss").format(timeNowL)
                    uiState.addMessage(
                        Message(authorMe, message, timeNow),
                    )
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
            Messages(
                messages = uiState.messages,
                navigateToProfile = navigateToProfile,
                modifier = Modifier.weight(1f),
                scrollState = scrollState,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun GreetingPreview() {
    AppDemoTheme {
        ComposeListScreen(navigateToProfile = {})
    }
}