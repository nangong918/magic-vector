package com.magicvector.demo.view

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.magicvector.demo.domain.vo.SelectItem
import com.magicvector.demo.ui.theme.*

@Composable
fun SelectItemList(
    items: List<SelectItem>,
    onItemClick: (SelectItem) -> Unit = {},
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
    ) {
        items(
            items = items,
            key = { it.id } // 重要：为每个item设置唯一key
        ) { item ->
            SelectItemView(
                title = item.title,
                subtitle = item.subtitle,
                onItemClick = { onItemClick(item) },
                icon = if (item.iconRes != null) {
                    {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "星标",
                            tint = Pink40
                        )
                    }
                } else null
            )

            // 添加分割线
            HorizontalDivider(
                modifier = Modifier.padding(start = 12.dp),
                thickness = 1.dp,
                color = Pink40
            )
        }
    }
}


// 预览数据
private val previewItems = listOf(
    SelectItem(
        id = "1",
        title = "选项一",
        subtitle = "这是第一个选项的详细描述信息",
        iconRes = 1 // 任意非空值，用于显示图标
    ),
    SelectItem(
        id = "2",
        title = "选项二",
        subtitle = "第二个选项的副标题内容",
        iconRes = null // 没有图标
    ),
    SelectItem(
        id = "3",
        title = "选项三",
        subtitle = "这是一个很长的副标题，用来测试文本过长时的显示效果，看看是否会正确换行",
        iconRes = 1
    ),
    SelectItem(
        id = "4",
        title = "选项四",
        subtitle = null, // 没有副标题
        iconRes = 1
    ),
    SelectItem(
        id = "5",
        title = "选项五",
        subtitle = "最后一个选项",
        iconRes = null
    )
)

// 基础预览
@Preview(showBackground = true, name = "带图标列表")
@Composable
fun SelectItemListWithIconsPreview() {
    AppDemoTheme {
        SelectItemList(
            items = previewItems,
            onItemClick = { item ->
                // 预览中的点击处理
                println("点击了: ${item.title}")
            },
            modifier = Modifier.padding(16.dp)
        )
    }
}

// 只有标题的预览
@Preview(showBackground = true, name = "简洁列表")
@Composable
fun SelectItemListSimplePreview() {
    val simpleItems = listOf(
        SelectItem(id = "1", title = "简洁选项一", subtitle = null, iconRes = null),
        SelectItem(id = "2", title = "简洁选项二", subtitle = null, iconRes = null),
        SelectItem(id = "3", title = "简洁选项三", subtitle = null, iconRes = null)
    )

    AppDemoTheme {
        SelectItemList(
            items = simpleItems,
            onItemClick = { },
            modifier = Modifier.padding(16.dp)
        )
    }
}

// 带图标无副标题的预览
@Preview(showBackground = true, name = "图标列表")
@Composable
fun SelectItemListIconsOnlyPreview() {
    val iconItems = listOf(
        SelectItem(id = "1", title = "图标选项一", subtitle = null, iconRes = 1),
        SelectItem(id = "2", title = "图标选项二", subtitle = null, iconRes = 1),
        SelectItem(id = "3", title = "图标选项三", subtitle = null, iconRes = 1)
    )

    AppDemoTheme {
        SelectItemList(
            items = iconItems,
            onItemClick = { },
            modifier = Modifier.padding(16.dp)
        )
    }
}

// 空列表预览
@Preview(showBackground = true, name = "空列表")
@Composable
fun SelectItemListEmptyPreview() {
    AppDemoTheme {
        SelectItemList(
            items = emptyList(),
            onItemClick = { },
            modifier = Modifier.padding(16.dp)
        )
    }
}

// 单一项预览
@Preview(showBackground = true, name = "单一项")
@Composable
fun SelectItemListSinglePreview() {
    val singleItem = listOf(
        SelectItem(
            id = "1",
            title = "单独选项",
            subtitle = "只有一个项目的列表",
            iconRes = 1
        )
    )

    AppDemoTheme {
        SelectItemList(
            items = singleItem,
            onItemClick = { },
            modifier = Modifier.padding(16.dp)
        )
    }
}
