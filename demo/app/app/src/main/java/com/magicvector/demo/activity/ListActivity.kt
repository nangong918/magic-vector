package com.magicvector.demo.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.magicvector.demo.view.message.SendMessageView

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
            // 预留发送消息区域
            SendMessagePlaceholder()
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
                    MessageList()
                }
        }
    }

}

@Composable
fun MessageList() {
    LazyColumn (
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 5.dp, end = 5.dp),
        // 对应 app:stackFromEnd="true"
        reverseLayout = true,
        verticalArrangement = Arrangement.Top
    ) {
        items(50) { index ->
            Text(
                text = "消息 $index",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun SendMessagePlaceholder() {
    SendMessageView(
        onSendClick = { message ->
            // 处理发送消息
            println("发送消息: $message")
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