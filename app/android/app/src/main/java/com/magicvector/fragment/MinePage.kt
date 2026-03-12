package com.magicvector.fragment

import android.content.Intent
import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.magicvector.viewModel.fragment.MineVm
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.magicvector.activity.test.ComposeTestActivity
import com.magicvector.viewModel.fragment.MineEffect
import com.magicvector.viewModel.fragment.MineIntent
import com.magicvector.viewModel.fragment.MineState
import com.view.appview.R
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MineScreen(
    modifier: Modifier = Modifier,
    viewModel: MineVm = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.processIntent(MineIntent.OnLocalVideoSelected(uri?.toString()))
    }

    LaunchedEffect(Unit) {
        viewModel.processIntent(MineIntent.Initialize)
    }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                MineEffect.OpenLocalVideoPicker -> pickerLauncher.launch("video/*")
                is MineEffect.NavigateToActivity -> {
                    if (effect.activityClassName == ComposeTestActivity::class.java.name) {
                        context.startActivity(Intent(context, ComposeTestActivity::class.java))
                    }
                }
            }
        }
    }

    MineContent(
        modifier = modifier,
        state = state,
        onTestClick = { viewModel.processIntent(MineIntent.TestButtonClick) },
        onPickLocalVideo = { viewModel.processIntent(MineIntent.PickLocalVideoClick) },
        onTogglePlay = { viewModel.processIntent(MineIntent.ToggleVideoPlay) }
    )
}


@Composable
fun MineContent(
    modifier: Modifier = Modifier,
    state: MineState,
    onTestClick: () -> Unit,
    onPickLocalVideo: () -> Unit,
    onTogglePlay: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(text = "Mine", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "视频")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onPickLocalVideo) {
                    Text("选择本地视频")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onTogglePlay) {
                    Text(if (state.isVideoPlaying) "暂停/停止" else "播放")
                }
                Spacer(modifier = Modifier.height(8.dp))
                LocalVideoView(
                    uriString = state.selectedVideoUri,
                    isPlaying = state.isVideoPlaying
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Setting")
                Text(state.settingTodo, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text("云视频")
                Text(state.cloudReplayTodo, style = MaterialTheme.typography.bodySmall)
                Text(state.uploadTodo, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onTestClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.test),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun LocalVideoView(
    uriString: String?,
    isPlaying: Boolean
) {
    if (uriString.isNullOrBlank()) {
        Text("未选择本地视频", style = MaterialTheme.typography.bodySmall)
        return
    }
    val playState = remember { mutableStateOf(false) }
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        factory = { context ->
            VideoView(context).apply {
                setVideoURI(Uri.parse(uriString))
            }
        },
        update = { videoView ->
            if (isPlaying && !playState.value) {
                videoView.setVideoURI(Uri.parse(uriString))
                videoView.start()
                playState.value = true
            } else if (!isPlaying && playState.value) {
                videoView.pause()
                playState.value = false
            }
        }
    )
}


// 预览函数：直接预览 MineContent，传入模拟的点击事件
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun MineContentPreview() {
    MineContent(
        modifier = Modifier,
        state = MineState(userName = "Demo"),
        onTestClick = {},
        onPickLocalVideo = {},
        onTogglePlay = {}
    )
}
