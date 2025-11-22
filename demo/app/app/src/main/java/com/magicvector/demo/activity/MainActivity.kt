package com.magicvector.demo.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.magicvector.demo.domain.vo.CatalogItem
import com.magicvector.demo.manager.CatalogManager
import com.magicvector.demo.manager.OnClickCatalogItem
import com.magicvector.demo.ui.theme.AppDemoTheme
import com.magicvector.demo.view.CatalogItemList

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
    val context = LocalContext.current
    val catalogItems = remember { CatalogManager.getCatalogItems() }

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
                placeholder = { Text("搜索功能...") },
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
                catalogItems
            } else {
                catalogItems.filter { item ->
                    item.title.contains(searchText, ignoreCase = true) ||
                            (item.subtitle?.contains(searchText, ignoreCase = true) == true)
                }
            }

            CatalogItemList(
                items = filteredItems,
                onItemClick = object : OnClickCatalogItem {
                    override fun onClick(item: CatalogItem) {
                        // 使用 CatalogManager 处理点击事件
                        val onClickListener = CatalogManager.onItemClick(item, context)
                        onClickListener.onClick(item)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )
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