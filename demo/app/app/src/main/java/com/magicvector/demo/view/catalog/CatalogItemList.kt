package com.magicvector.demo.view.catalog

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
import com.magicvector.demo.activity.NetworkActivity
import com.magicvector.demo.activity.ui.theme.AppDemoTheme
import com.magicvector.demo.activity.ui.theme.Pink40
import com.magicvector.demo.domain.vo.CatalogItem
import com.magicvector.demo.manager.OnClickCatalogItem

@Composable
fun CatalogItemList(
    items: List<CatalogItem>,
    onItemClick: OnClickCatalogItem,
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
                onItemClick = { onItemClick.onClick(item) },
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
                thickness = 1.dp,
                color = Pink40
            )
        }
    }
}


// 预览数据
private val previewItems = listOf(
    CatalogItem(
        id = "1",
        title = "选项一",
        subtitle = "这是第一个选项的详细描述信息",
        iconRes = 1,
        cls = NetworkActivity::class.java
    ),
    CatalogItem(
        id = "2",
        title = "选项二",
        subtitle = "第二个选项的副标题内容",
        iconRes = null,
        cls = NetworkActivity::class.java
    ),
    CatalogItem(
        id = "3",
        title = "选项三",
        subtitle = "这是一个很长的副标题，用来测试文本过长时的显示效果，看看是否会正确换行",
        iconRes = 1,
        cls = NetworkActivity::class.java
    ),
    CatalogItem(
        id = "4",
        title = "选项四",
        subtitle = null, // 没有副标题
        iconRes = 1,
        cls = NetworkActivity::class.java
    ),
    CatalogItem(
        id = "5",
        title = "选项五",
        subtitle = "最后一个选项",
        iconRes = null,
        cls = NetworkActivity::class.java
    )
)

// 基础预览
@Preview(showBackground = true, name = "带图标列表")
@Composable
fun SelectItemListWithIconsPreview() {
    AppDemoTheme {
        CatalogItemList(
            items = previewItems,
            onItemClick = object : OnClickCatalogItem {
                override fun onClick(item: CatalogItem) {
                    // 预览中的点击处理
                    println("点击了: ${item.title}")
                }
            }
        )
    }
}

// 只有标题的预览
@Preview(showBackground = true, name = "简洁列表")
@Composable
fun SelectItemListSimplePreview() {
    val simpleItems = listOf(
        CatalogItem(id = "1", title = "简洁选项一", subtitle = null, iconRes = null,
            cls = NetworkActivity::class.java),
        CatalogItem(id = "2", title = "简洁选项二", subtitle = null, iconRes = null,
            cls = NetworkActivity::class.java),
        CatalogItem(id = "3", title = "简洁选项三", subtitle = null, iconRes = null,
            cls = NetworkActivity::class.java)
    )

    AppDemoTheme {
        CatalogItemList(
            items = simpleItems,
            onItemClick = object : OnClickCatalogItem {
                override fun onClick(item: CatalogItem) {
                }
            },
            modifier = Modifier.padding(16.dp)
        )
    }
}

// 带图标无副标题的预览
@Preview(showBackground = true, name = "图标列表")
@Composable
fun SelectItemListIconsOnlyPreview() {
    val iconItems = listOf(
        CatalogItem(id = "1", title = "图标选项一", subtitle = null, iconRes = 1,
            cls = NetworkActivity::class.java),
        CatalogItem(id = "2", title = "图标选项二", subtitle = null, iconRes = 1,
            cls = NetworkActivity::class.java),
        CatalogItem(id = "3", title = "图标选项三", subtitle = null, iconRes = 1,
            cls = NetworkActivity::class.java)
    )

    AppDemoTheme {
        CatalogItemList(
            items = iconItems,
            onItemClick = object : OnClickCatalogItem {
                override fun onClick(item: CatalogItem) {
                }
            },
            modifier = Modifier.padding(16.dp)
        )
    }
}

// 单一项预览
@Preview(showBackground = true, name = "单一项")
@Composable
fun SelectItemListSinglePreview() {
    val singleItem = listOf(
        CatalogItem(
            id = "1",
            title = "单独选项",
            subtitle = "只有一个项目的列表",
            iconRes = 1,
            cls = NetworkActivity::class.java
        )
    )

    AppDemoTheme {
        CatalogItemList(
            items = singleItem,
            onItemClick = object : OnClickCatalogItem {
                override fun onClick(item: CatalogItem) {
                }
            },
            modifier = Modifier.padding(16.dp)
        )
    }
}
