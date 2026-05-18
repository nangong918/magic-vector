package com.vectordemo.ui.view.activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vectordemo.viewModel.activity.MainIntent
import com.vectordemo.viewModel.activity.MainState

@Composable
fun MainScreen(
    state: MainState,
    processIntent: (MainIntent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("当前用户：${state.displayUserName}")
            TextButton(onClick = { processIntent(MainIntent.Logout) }) { Text("登出") }
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = { processIntent(MainIntent.UpdateQuery(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search Demo") },
            singleLine = true,
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.items, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable {
                        processIntent(MainIntent.ClickDemo(item.route))
                    },
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(item.title)
                        Text(item.subtitle)
                    }
                }
            }
        }
    }
}
