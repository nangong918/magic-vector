package com.vectordemo.ui.view.oss

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import coil.compose.AsyncImage
import com.vectordemo.domain.dto.http.response.OssUserBucketFileItemRow
import com.vectordemo.viewModel.oss.OssDemoViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OssDemoScreen(
    vm: OssDemoViewModel,
    onBack: () -> Unit
) {
    val ui by vm.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(ui.toast) {
        ui.toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.consumeToast()
        }
    }

    if (ui.touristBlocked) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("OSS Demo") },
                    navigationIcon = {
                        TextButton(onClick = onBack) { Text("返回") }
                    }
                )
            }
        ) { pad ->
            Box(
                Modifier
                    .padding(pad)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("游客或未登录无法使用 OSS", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBack) { Text("返回") }
                }
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    val pickMain = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        pickedUri = uri
    }
    var replaceContext by remember { mutableStateOf<Pair<String, String>?>(null) }
    val pickReplace = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val ctx = replaceContext
        if (uri != null && ctx != null) {
            vm.replaceFile(ctx.first, ctx.second, uri)
        }
        replaceContext = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OSS Demo") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("返回") }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text("上传") }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text("存储桶") }
                )
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> OssUploadPage(
                        pickedUri = pickedUri,
                        onPickClick = {
                            pickMain.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onUploadClick = {
                            val u = pickedUri
                            if (u == null) {
                                Toast.makeText(context, "请先选择图片", Toast.LENGTH_SHORT).show()
                            } else {
                                vm.uploadImage(u)
                            }
                        }
                    )

                    else -> OssBucketPage(
                        ui = ui,
                        isRefreshing = ui.loadingBuckets,
                        onRefreshBuckets = { vm.refreshBuckets() },
                        onToggleBucket = { vm.toggleBucket(it) },
                        onDownload = { url, name -> vm.saveImageToGallery(url, name) },
                        onReplace = { bucket, fileId ->
                            replaceContext = fileId to bucket
                            pickReplace.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onDelete = { bucket, fileId -> vm.deleteFile(fileId, bucket) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OssUploadPage(
    pickedUri: Uri?,
    onPickClick: () -> Unit,
    onUploadClick: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .combinedClickable(onClick = onPickClick, onLongClick = {}),
            contentAlignment = Alignment.Center
        ) {
            if (pickedUri == null) {
                Text("点击选择相册图片", style = MaterialTheme.typography.bodyLarge)
            } else {
                AsyncImage(
                    model = pickedUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
        Button(
            onClick = onUploadClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("上传")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OssBucketPage(
    ui: com.vectordemo.viewModel.oss.OssDemoUiState,
    isRefreshing: Boolean,
    onRefreshBuckets: () -> Unit,
    onToggleBucket: (String) -> Unit,
    onDownload: (String, String) -> Unit,
    onReplace: (String, String) -> Unit,
    onDelete: (String, String) -> Unit
) {
    var menuTarget by remember { mutableStateOf<Pair<String, OssUserBucketFileItemRow>?>(null) }

    LaunchedEffect(Unit) {
        onRefreshBuckets()
    }

    if (menuTarget != null) {
        val bucket = menuTarget!!.first
        val row = menuTarget!!.second
        val fid = row.fileId ?: ""
        AlertDialog(
            onDismissRequest = { menuTarget = null },
            title = { Text(row.originFileName ?: "文件") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            val url = row.url ?: ""
                            if (url.isNotBlank()) {
                                onDownload(url, row.originFileName ?: "image.jpg")
                            }
                            menuTarget = null
                        }
                    ) { Text("下载到本地") }
                    TextButton(
                        onClick = {
                            menuTarget = null
                            onReplace(bucket, fid)
                        }
                    ) { Text("更换图片") }
                    TextButton(
                        onClick = {
                            onDelete(bucket, fid)
                            menuTarget = null
                        }
                    ) { Text("删除图片") }
                    TextButton(onClick = { menuTarget = null }) { Text("取消") }
                }
            },
            confirmButton = {
                TextButton(onClick = { menuTarget = null }) { Text("关闭") }
            }
        )
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefreshBuckets,
        modifier = Modifier.fillMaxSize(),
    ) {
        if (ui.loadingBuckets && ui.buckets.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ui.buckets, key = { it }) { bucket ->
                    BucketSection(
                        bucket = bucket,
                        expanded = ui.expandedBuckets.contains(bucket),
                        loading = ui.loadingBucket == bucket,
                        files = ui.filesByBucket[bucket].orEmpty(),
                        onHeaderClick = { onToggleBucket(bucket) },
                        onFileLongPress = { row ->
                            menuTarget = bucket to row
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BucketSection(
    bucket: String,
    expanded: Boolean,
    loading: Boolean,
    files: List<OssUserBucketFileItemRow>,
    onHeaderClick: () -> Unit,
    onFileLongPress: (OssUserBucketFileItemRow) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onHeaderClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "▼ $bucket" else "▶ $bucket",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (loading) {
                    CircularProgressIndicator(Modifier.width(22.dp).height(22.dp), strokeWidth = 2.dp)
                }
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                if (files.isEmpty() && !loading) {
                    Text("暂无文件", style = MaterialTheme.typography.bodySmall)
                } else {
                    files.forEach { row ->
                        FileTile(row = row, onLongPress = { onFileLongPress(row) })
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FileTile(
    row: OssUserBucketFileItemRow,
    onLongPress: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = row.url,
            contentDescription = row.originFileName,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = row.originFileName ?: row.fileId ?: "",
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

