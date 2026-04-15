package com.vectordemo.ui.view.activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vectordemo.domain.model.demo.DemoCatalogItem
import com.vectordemo.domain.model.demo.DemoRoute
import com.vectordemo.ui.theme.VectorDemoTheme
import com.vectordemo.viewModel.activity.MainIntent
import com.vectordemo.viewModel.activity.MainState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainState,
    processIntent: (MainIntent) -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Demo Catalog") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "当前用户：${state.displayUserName}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = { showLogoutDialog = true }) {
                    Text("登出")
                }
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = { processIntent(MainIntent.UpdateQuery(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                label = { Text("Search Demo") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "search") },
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.items, key = { it.id }) { item ->
                    DemoBar(
                        item = item,
                        onClick = { processIntent(MainIntent.ClickDemo(item.route)) }
                    )
                }
            }
        }
    }
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("确认登出") },
            text = { Text("是否退出当前账号并返回登录页？") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    processIntent(MainIntent.Logout)
                }) { Text("登出") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun DemoBar(
    item: DemoCatalogItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = item.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = item.subtitle,
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 13.sp
            )
            Text(
                text = ">",
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.End),
                fontSize = 18.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    VectorDemoTheme {
        MainScreen(
            state = MainState(
                query = "",
                items = listOf(
                    DemoCatalogItem(
                        id = "hello",
                        title = "Hello Demo",
                        subtitle = "Compose + MVI navigation sample",
                        route = DemoRoute.HELLO
                    )
                )
            ),
            processIntent = {}
        )
    }
}
