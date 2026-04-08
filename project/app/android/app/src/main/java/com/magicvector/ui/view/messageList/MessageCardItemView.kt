package com.magicvector.ui.view.messageList


import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.compose.AsyncImage

@SuppressLint("ModifierParameter")
@Composable
fun MessageListItem(
    avatarUrl: String? = null,
    name: String,
    messagePreview: String,
    time: String,
    unreadCount: Int = 0,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 8.dp)
    ) {
        val (avatar, nameText, messageText, timeText, divider, messagePrompt) = createRefs()

        // 头像
        AsyncImage(
            model = avatarUrl ?: com.view.appview.R.mipmap.logo,
            contentDescription = stringResource(id = com.view.appview.R.string.avatar_image),
            modifier = Modifier
                .size(45.dp)
                .clip(CircleShape)
                .constrainAs(avatar) {
                    start.linkTo(parent.start, margin = 8.dp)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
            contentScale = ContentScale.Crop,
            error = painterResource(id = com.view.appview.R.mipmap.logo),
            placeholder = painterResource(id = com.view.appview.R.mipmap.logo)
        )

        // 用户名
        Text(
            text = name,
            fontSize = 16.sp,
            color = Color.Black,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.constrainAs(nameText) {
                start.linkTo(avatar.end, margin = 10.dp)
                top.linkTo(parent.top, margin = 8.dp)
                end.linkTo(messagePrompt.start, margin = 18.dp)
                width = Dimension.fillToConstraints
            }
        )

        // 消息提示数字
        MessagePromptView(
            count = unreadCount,
            modifier = Modifier
                .size(21.dp)
                .constrainAs(messagePrompt) {
                    top.linkTo(nameText.top)
                    bottom.linkTo(nameText.bottom)
                    end.linkTo(parent.end, margin = 10.dp)
                }
        )

        // 时间
        Text(
            text = time,
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.constrainAs(timeText) {
                end.linkTo(parent.end, margin = 8.dp)
                bottom.linkTo(parent.bottom, margin = 8.dp)
            }
        )

        // 消息预览
        Text(
            text = messagePreview,
            fontSize = 14.sp,
            color = Color.DarkGray,
            maxLines = 1,
            modifier = Modifier.constrainAs(messageText) {
                start.linkTo(avatar.end, margin = 10.dp)
                top.linkTo(nameText.bottom, margin = 4.dp)
                end.linkTo(timeText.start, margin = 20.dp)
                bottom.linkTo(parent.bottom, margin = 5.dp)
                width = Dimension.fillToConstraints
            }
        )

        // 分隔线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.LightGray)
                .constrainAs(divider) {
                    top.linkTo(avatar.bottom, margin = 8.dp)
                    bottom.linkTo(parent.bottom)
                }
        )
    }
}

@SuppressLint("ModifierParameter")
// 简化版本（使用 Row + Column 布局）
@Composable
fun SimpleMessageListItem(
    avatarUrl: String? = null,
    name: String,
    messagePreview: String,
    time: String,
    unreadCount: Int = 0,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像
        AsyncImage(
            model = avatarUrl ?: com.view.appview.R.mipmap.logo,
            contentDescription = stringResource(id = com.view.appview.R.string.avatar_image),
            modifier = Modifier
                .size(45.dp)
                .clip(CircleShape)
                .padding(start = 8.dp),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(10.dp))

        // 中间内容区域
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // 第一行：用户名 + 消息提示
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = name,
                    fontSize = 16.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                MessagePromptView(
                    count = unreadCount,
                    modifier = Modifier.size(21.dp)
                )
            }

            // 第二行：消息预览
            Text(
                text = messagePreview,
                fontSize = 14.sp,
                color = Color.DarkGray,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 时间
        Text(
            text = time,
            fontSize = 14.sp,
            color = Color.Gray
        )
    }

    // 分隔线
    HorizontalDivider(
        modifier = Modifier.padding(top = 8.dp),
        thickness = 1.dp,
        color = Color.LightGray
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun MessageListItemPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "消息列表项预览",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 各种状态的预览
        MessageListItem(
            name = "张三",
            messagePreview = "你好，最近怎么样？",
            time = "10:30",
            unreadCount = 0
        )

        MessageListItem(
            name = "李四",
            messagePreview = "文件已发送，请查收",
            time = "09:15",
            unreadCount = 2
        )

        MessageListItem(
            name = "系统通知",
            messagePreview = "你的Agent已创建成功",
            time = "昨天",
            unreadCount = 5
        )

        MessageListItem(
            name = "王五",
            messagePreview = "这是一条非常长的消息预览，用来测试换行效果，应该只显示一行并省略",
            time = "2024-01-01",
            unreadCount = 99
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SimpleMessageListItemPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "简化版消息列表项预览",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        SimpleMessageListItem(
            name = "张三",
            messagePreview = "你好，最近怎么样？",
            time = "10:30",
            unreadCount = 0
        )

        SimpleMessageListItem(
            name = "李四",
            messagePreview = "文件已发送，请查收",
            time = "09:15",
            unreadCount = 2
        )

        SimpleMessageListItem(
            name = "系统通知",
            messagePreview = "你的Agent已创建成功",
            time = "昨天",
            unreadCount = 5
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun MessageListPreview() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(10) { index ->
            MessageListItem(
                name = "用户 $index",
                messagePreview = "这是第 $index 条消息的预览内容",
                time = when (index % 3) {
                    0 -> "10:30"
                    1 -> "昨天"
                    else -> "2024-01-0${index}"
                },
                unreadCount = when (index % 4) {
                    0 -> 0
                    1 -> 3
                    2 -> 15
                    else -> 99
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MessageListItemStatesPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "不同状态的消息项",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 已读消息
        MessageListItem(
            name = "已读消息",
            messagePreview = "这是已读消息的预览",
            time = "10:30",
            unreadCount = 0
        )

        // 未读消息
        MessageListItem(
            name = "未读消息",
            messagePreview = "你有3条未读消息",
            time = "09:15",
            unreadCount = 3
        )

        // 大量未读
        MessageListItem(
            name = "大量未读",
            messagePreview = "这是一个非常活跃的对话",
            time = "昨天",
            unreadCount = 99)
    }

    // 超长用户名
    MessageListItem(
        name = "这是一个非常长的用户名用来测试换行效果",
        messagePreview = "短消息预览",
        time = "2024-01-01",
        unreadCount = 5
    )

    // 超长消息预览
    MessageListItem(
        name = "普通用户",
        messagePreview = "这是一条非常非常长的消息预览内容，应该只显示一行并且在末尾显示省略号，这样才能保持界面整洁",
        time = "2024-01-01",
        unreadCount = 1
    )
}
