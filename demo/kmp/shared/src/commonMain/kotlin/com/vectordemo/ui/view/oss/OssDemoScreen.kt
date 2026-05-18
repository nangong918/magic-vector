package com.vectordemo.ui.view.oss

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vectordemo.viewModel.oss.OssDemoIntent
import com.vectordemo.viewModel.oss.OssDemoState

@Composable
fun OssDemoScreen(
    state: OssDemoState,
    processIntent: (OssDemoIntent) -> Unit,
    onBack: () -> Unit,
) {
    if (state.touristBlocked) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("游客或未登录无法使用 OSS")
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBack) { Text("返回") }
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("OSS Demo")
            Button(onClick = onBack) { Text("返回") }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { processIntent(OssDemoIntent.RequestMainImagePick) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.pickedUploadFile == null) "选择图片" else "已选择图片")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { processIntent(OssDemoIntent.UploadSubmit) }, modifier = Modifier.fillMaxWidth()) { Text("上传") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { processIntent(OssDemoIntent.RefreshBuckets) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.loadingBuckets) "刷新中..." else "刷新存储桶")
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
            items(state.buckets, key = { it }) { bucket ->
                val expanded = state.expandedBuckets.contains(bucket)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable {
                        processIntent(OssDemoIntent.ToggleBucket(bucket))
                    },
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(if (expanded) "▼ $bucket" else "▶ $bucket")
                        if (expanded) {
                            state.filesByBucket[bucket].orEmpty().forEach { file ->
                                Column(
                                    Modifier.fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                ) {
                                    Text(file.originFileName.ifBlank { file.fileId.toString() })
                                    Text(file.url)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Button(onClick = {
                                            processIntent(OssDemoIntent.DownloadImage(file.url, file.originFileName))
                                        }) { Text("下载") }
                                        Button(onClick = {
                                            processIntent(OssDemoIntent.RequestReplacePick(bucket, file.fileId))
                                        }) { Text("更换") }
                                        Button(onClick = {
                                            processIntent(OssDemoIntent.DeleteFile(bucket, file.fileId))
                                        }) { Text("删除") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
