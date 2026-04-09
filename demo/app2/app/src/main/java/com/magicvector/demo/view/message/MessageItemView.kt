package com.magicvector.demo.view.message

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.magicvector.demo.R
import com.magicvector.demo.activity.ui.theme.*
import com.magicvector.demo.activity.ui.theme.AppDemoTheme

@Composable
fun ReceivedMessage(
    modifier: Modifier = Modifier,
    avatarUrl: String? = null, // 头像URL
    messageText: String = "test text...",
    messageImageUrl: String? = null, // 消息图片URL
    timeText: String = "2025/10/9",
    isShowImage: Boolean = false
) {
    Row(
        modifier = modifier
            .padding(4.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        // 头像 - 使用 AsyncImage 加载网络图片
        AsyncImage(
            model = avatarUrl ?: R.mipmap.logo, // 支持 URL 或本地资源
            contentDescription = "头像",
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape),
            placeholder = painterResource(R.mipmap.logo), // 占位图
            error = painterResource(R.mipmap.logo) // 错误图
        )

        Spacer(modifier = Modifier.width(5.dp))

        // 消息内容区域
        Column(
            modifier = Modifier
                .weight(1f)
                .wrapContentWidth(Alignment.Start)
        ) {
            // 消息气泡
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .widthIn(max = 300.dp)
                    .background(
                        color = A1_75, // 使用 A1_75 颜色
                        shape = RoundedCornerShape(
                            topStart = 0.dp,      // 左上直角
                            topEnd = 24.dp,       // 右上圆角
                            bottomStart = 24.dp,  // 左下圆角
                            bottomEnd = 24.dp     // 右下圆角
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    // 消息图片 - 使用 AsyncImage 加载网络图片
                    if (isShowImage && messageImageUrl != null) {
                        AsyncImage(
                            model = messageImageUrl,
                            contentDescription = "消息图片",
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(R.drawable.default_img), // 加载中占位图
                            error = painterResource(R.drawable.error_img) // 加载失败图
                        )
                    }

                    // 消息文本
                    Text(
                        text = messageText,
                        color = S1_800,
                        fontSize = 17.sp,
                        modifier = Modifier.padding(horizontal = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 时间
            Text(
                text = timeText,
                color = S1_800,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.Start)
            )
        }
    }
}


@Composable
fun SentMessage(
    modifier: Modifier = Modifier,
    messageText: String = "test text...",
    messageImageUrl: String? = null,
    timeText: String = "2025/10/9",
    isShowImage: Boolean = false
) {
    Row(
        modifier = modifier
            .padding(4.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.End // 整体靠右
    ) {
        // 消息内容区域
        Column(
            modifier = Modifier
                .wrapContentWidth()
                .widthIn(max = 300.dp),
            horizontalAlignment = Alignment.End // 内容靠右
        ) {
            // 消息气泡
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .background(
                        color = A1_75,
                        shape = RoundedCornerShape(
                            topStart = 24.dp,    // 左上圆角
                            topEnd = 0.dp,       // 右上直角
                            bottomStart = 24.dp, // 左下圆角
                            bottomEnd = 24.dp    // 右下圆角
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End // 气泡内内容靠右
                ) {
                    // 消息图片
                    if (isShowImage && messageImageUrl != null) {
                        AsyncImage(
                            model = messageImageUrl,
                            contentDescription = "消息图片",
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(R.drawable.default_img),
                            error = painterResource(R.drawable.error_img)
                        )
                    }

                    // 消息文本
                    Text(
                        text = messageText,
                        color = S1_800,
                        fontSize = 17.sp,
                        modifier = Modifier.padding(horizontal = 5.dp),
                        textAlign = TextAlign.Start
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 时间 - 靠右对齐
            Text(
                text = timeText,
                color = S1_800,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.End) // 时间靠右
            )
        }

        Spacer(modifier = Modifier.width(5.dp)) // 右边留点间距
    }
}

// 预览
@Preview(showBackground = true, widthDp = 360)
@Composable
fun SentMessagePreview() {
    AppDemoTheme {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            SentMessage(
                messageText = "这是一条发送的消息",
                timeText = "2025/10/9 10:30",
                isShowImage = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            ReceivedMessage(
                messageText = "这是一条收到的消息",
                timeText = "2025/10/9 10:31",
                isShowImage = false
            )
        }
    }
}


@Preview(showBackground = true, widthDp = 360)
@Composable
fun ReceivedMessageLongTextPreview() {
    AppDemoTheme {
        ReceivedMessage(
            messageText = "这是一条非常长的消息内容，用来测试消息气泡的宽度限制和文本换行效果，确保布局在各种情况下都能正常显示",
            timeText = "2025/10/9 10:32",
            isShowImage = false
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
fun SentMessageLongTextPreview() {
    AppDemoTheme {
        SentMessage(
            messageText = "这是一条非常长的消息内容，用来测试消息气泡的宽度限制和文本换行效果，确保布局在各种情况下都能正常显示",
            timeText = "2025/10/9 10:32",
            isShowImage = false
        )
    }
}



