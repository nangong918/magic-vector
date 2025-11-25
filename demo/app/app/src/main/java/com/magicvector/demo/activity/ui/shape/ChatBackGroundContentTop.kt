package com.magicvector.demo.activity.ui.shape

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.magicvector.demo.activity.ui.theme.*
import com.magicvector.demo.activity.ui.theme.AppDemoTheme


// 定义常用形状
val BottomRoundedShape = RoundedCornerShape(
    topStart = 0.dp,
    topEnd = 0.dp,
    bottomStart = 32.dp,
    bottomEnd = 32.dp
)

// 扩展函数方式封装
@SuppressLint("SuspiciousModifierThen")
fun Modifier.bottomRoundedBackground(
    color: Color = A1_10,
    cornerRadius: Dp = 32.dp
): Modifier = this.then(
    clip(
        RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = cornerRadius,
            bottomEnd = cornerRadius
        )
    ).background(color)
)

// 更通用的版本
@SuppressLint("SuspiciousModifierThen")
fun Modifier.roundedBackground(
    topStart: Dp = 0.dp,
    topEnd: Dp = 0.dp,
    bottomStart: Dp = 32.dp,
    bottomEnd: Dp = 32.dp,
    color: Color = A1_10
): Modifier = this.then(
    clip(
        RoundedCornerShape(
            topStart = topStart,
            topEnd = topEnd,
            bottomStart = bottomStart,
            bottomEnd = bottomEnd
        )
    ).background(color)
)

@Preview(showBackground = true)
@Composable
fun BottomRoundedShapePreview() {
    AppDemoTheme {
        Box(
            modifier = Modifier
                .size(200.dp, 100.dp)
                .bottomRoundedBackground(color = A1_100)
        ) {
            Text("底部圆角")
        }
    }
}