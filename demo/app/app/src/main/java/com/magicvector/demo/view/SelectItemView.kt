package com.magicvector.demo.view

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magicvector.demo.ui.theme.*

@Composable
fun SelectItemView(
    // 参数定义
    title: String,
    subtitle: String? = null, // 新增副标题参数
    onItemClick: () -> Unit = {},
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    // 其他自定义参数
    icon: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(12.dp)
    ) {
        // 第一行：图标和标题水平排列
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧：图标和标题
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧图标（可选）
                if (icon != null) {
                    icon()
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // 标题
                Text(
                    text = title,
                    fontSize = 18.sp,
                    color = Purple40
                )
            }
        }

        // 第二行：副标题（在图标和标题下方）
        if (!subtitle.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = PurpleGrey80,
                modifier = Modifier.padding(start = if (icon != null) 36.dp else 0.dp) // 与标题对齐
            )
        }
    }
}

// 基础预览
@Preview(showBackground = true)
@Composable
fun SelectItemViewPreview() {
    SelectItemView(
        title = "选项一",
        subtitle = "这是选项的副标题",
        onItemClick = {}
    )
}

// 未选中状态的预览
@Preview(showBackground = true)
@Composable
fun SelectItemViewNotSelectedPreview() {
    SelectItemView(
        title = "选项二",
        subtitle = "这是选项的副标题",
        onItemClick = {}
    )
}

// 带图标的预览
@Preview(showBackground = true)
@Composable
fun SelectItemViewWithIconPreview() {
    SelectItemView(
        title = "带图标的选项",
        subtitle = "这是选项的副标题",
        onItemClick = {},
        icon = {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "图标",
                tint = Pink40
            )
        }
    )
}

// 带自定义右侧内容的预览
@Preview(showBackground = true)
@Composable
fun SelectItemViewWithTrailingPreview() {
    SelectItemView(
        title = "带右侧内容的选项",
        subtitle = "这是选项的副标题",
        onItemClick = {},
    )
}

// 多个状态组合预览
@Preview(showBackground = true, name = "Multiple States")
@Composable
fun SelectItemViewMultiplePreview() {
    Column {
        SelectItemView(
            title = "选中状态",
            subtitle = "这是选项的副标题",
            onItemClick = {}
        )
        SelectItemView(
            title = "未选中状态",
            subtitle = "这是选项的副标题",
            onItemClick = {}
        )
        SelectItemView(
            title = "带图标选中",
            subtitle = "这是选项的副标题这是选项的副标题这是选项的副标题这是选项的副标题这是选项的副标题这是选项的副标题这是选项的副标题这是选项的副标题这是选项的副标题这是选项的副标题这是选项的副标题这是选项的副标题这是选项的副标题这是选项的副标题这是选项的副标题",
            onItemClick = {},
            icon = {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "星标",
                    tint = Pink40
                )
            }
        )
    }
}