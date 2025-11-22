package com.magicvector.demo.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.magicvector.demo.domain.vo.SelectItem
import com.magicvector.demo.ui.theme.AppDemoTheme
import com.magicvector.demo.view.SelectItemView

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppDemoTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    // 模拟数据
    val items = remember {
        listOf(
            SelectItem(
                id = "1",
                title = "选项一",
                subtitle = "这是第一个选项的详细描述信息",
                iconRes = 1
            ),
            SelectItem(
                id = "2",
                title = "选项二",
                subtitle = "第二个选项的副标题内容",
                iconRes = null
            ),
            SelectItem(
                id = "3",
                title = "选项三",
                subtitle = "这是一个很长的副标题，用来测试文本过长时的显示效果",
                iconRes = 1
            ),
            SelectItem(
                id = "4",
                title = "选项四",
                subtitle = null,
                iconRes = 1
            ),
            SelectItem(
                id = "5",
                title = "选项五",
                subtitle = "最后一个选项",
                iconRes = null
            ),
            SelectItem(
                id = "6",
                title = "Android开发",
                subtitle = "移动应用开发"
            ),
            SelectItem(
                id = "7",
                title = "Kotlin编程",
                subtitle = "现代编程语言"
            ),
            SelectItem(
                id = "8",
                title = "Jetpack Compose",
                subtitle = "声明式UI框架"
            )
        )
    }

    var searchText by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 搜索栏
            SearchBar(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                query = searchText,
                onQueryChange = { searchText = it },
                onSearch = { active = false },
                active = active,
                onActiveChange = { active = it },
                placeholder = { Text("搜索选项...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索"
                    )
                }
            ) {
                // 搜索建议（可选）
            }

            // 列表内容
            val filteredItems = if (searchText.isBlank()) {
                items
            } else {
                items.filter { item ->
                    item.title.contains(searchText, ignoreCase = true) ||
                            (item.subtitle?.contains(searchText, ignoreCase = true) == true)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                items(
                    items = filteredItems,
                    key = { it.id }
                ) { item ->
                    SelectItemView(
                        title = item.title,
                        subtitle = item.subtitle,
                        onItemClick = {
                            // 处理点击事件
                            // 可以显示Toast或跳转页面
                        },
                        icon = if (item.iconRes != null) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "星标",
                                    tint = com.magicvector.demo.ui.theme.Pink40
                                )
                            }
                        } else null
                    )

                    // 分割线
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 12.dp),
                        thickness = 0.5.dp,
                        color = com.magicvector.demo.ui.theme.Pink40
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun MainScreenPreview() {
    AppDemoTheme {
        MainScreen()
    }
}