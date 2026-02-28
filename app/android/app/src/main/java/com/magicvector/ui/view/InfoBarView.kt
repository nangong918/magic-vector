package com.magicvector.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.view.appview.R


@Composable
fun InfoBarView(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    height: Int = 50,
    showBackButton: Boolean = true,
    onBackClick: (() -> Unit)? = null,
    backIconTint: Color = colorResource(id = R.color.s1_800),
    title: String = "",
    titleColor: Color = Color.White,
    rightAction: @Composable (() -> Unit)? = null
) {
    Box(modifier = modifier
        .fillMaxWidth()
        .height(height = height.dp)
        .background(backgroundColor)
    ) {
        if (showBackButton && onBackClick != null) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .size(30.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.xml.chevron_left_24px),
                    contentDescription = "back",
                    tint = backIconTint
                )
            }
        }

        // 标题（居中）
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            modifier = Modifier.align(Alignment.Center)
        )

        // 右侧自定义操作
        if (rightAction != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            ) {
                rightAction()
            }
        }
    }
}



@Preview
@Composable
fun InfoBarViewPreview() {
    InfoBarView(
        showBackButton = true,
        onBackClick = {},
        title = "标题",
        titleColor = Color.White,
        backgroundColor = Color.Gray,
        rightAction = {
            IconButton(onClick = { /* 打开搜索 */ }) {
                Icon(
                    painter = painterResource(id = R.xml.settings_24px),
                    contentDescription = "搜索"
                )
            }
        }
    )
}










