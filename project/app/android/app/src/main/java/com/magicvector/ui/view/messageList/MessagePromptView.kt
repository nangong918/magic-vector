package com.magicvector.ui.view.messageList



import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.view.appview.R

@Composable
fun MessagePromptView(
    count: Int,
    modifier: Modifier = Modifier,
    maxDisplayCount: Int = 99,
    backgroundColor: Color = colorResource(id = R.color.red_1000),
    textColor: Color = Color.White,
    textSize: Int = 11, // sp
) {
    // 根据数量决定显示状态
    val displayState = when {
        count <= 0 -> MessagePromptState.Hidden
        count <= maxDisplayCount -> MessagePromptState.ShowNumber(count)
        else -> MessagePromptState.ShowOverflow(maxDisplayCount)
    }

    // 只有在需要显示时才渲染
    if (displayState != MessagePromptState.Hidden) {
        Box(
            modifier = modifier
                .size(21.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayState.displayText,
                fontSize = textSize.sp,
                color = textColor,
                maxLines = 1
            )
        }
    }
}

// 状态封装，便于管理显示逻辑
sealed class MessagePromptState {
    object Hidden : MessagePromptState()
    data class ShowNumber(val count: Int) : MessagePromptState()
    data class ShowOverflow(val maxCount: Int) : MessagePromptState()

    val displayText: String
        get() = when (this) {
            is Hidden -> ""
            is ShowNumber -> count.toString()
            is ShowOverflow -> "$maxCount+"
        }
}

// 如果需要状态管理（类似原生 View 的 setMessageNum）
@Composable
fun rememberMessagePromptState(initialCount: Int = 0) =
    rememberSaveable { mutableIntStateOf(initialCount) }


@SuppressLint("ModifierParameter")
// 带状态管理的版本（更接近原生 View 的使用方式）
@Composable
fun StatefulMessagePromptView(
    count: Int,
    onCountChange: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    maxDisplayCount: Int = 99,
    backgroundColor: Color = colorResource(id = R.color.red_1000),
    textColor: Color = Color.White,
    textSize: Int = 11,
) {
    MessagePromptView(
        count = count,
        modifier = modifier,
        maxDisplayCount = maxDisplayCount,
        backgroundColor = backgroundColor,
        textColor = textColor,
        textSize = textSize
    )
}

// 使用示例的 Composable
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun MessagePromptExample() {
    var messageCount by rememberMessagePromptState(5)

    Column {
        StatefulMessagePromptView(
            count = messageCount,
            modifier = Modifier.size(21.dp)
        )

        // 控制按钮
        Row {
            Button(onClick = { messageCount++ }) {
                Text("增加")
            }
            Button(onClick = { messageCount-- }) {
                Text("减少")
            }
            Button(onClick = { messageCount = 150 }) {
                Text("设为150")
            }
        }
    }
}