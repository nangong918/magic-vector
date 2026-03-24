package com.magicvector.ui.view.activity

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.viewModel.activity.MineVideoState
import com.magicvector.viewModel.activity.MineVideoTabState

@Composable
fun ComposeMineVideoActivityScreen(
    state: MineVideoState,
    onSwitchVideoTab: (MineVideoTabState) -> Unit,
    onPickLocalVideo: () -> Unit,
    onTogglePlay: () -> Unit,
    onLoadCloudVideos: () -> Unit,
    onOpenCloudVideo: (String) -> Unit,
    onUploadPick: () -> Unit,
    onPauseUpload: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MineVideoProfileCard(userName = state.userName)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MineVideoTabState.entries.forEach {
                        FilterChip(
                            selected = state.videoTab == it,
                            onClick = { onSwitchVideoTab(it) },
                            label = { Text(it.name) }
                        )
                    }
                }
                when (state.videoTab) {
                    MineVideoTabState.CLOUD -> {
                        Button(onClick = onLoadCloudVideos) { Text("刷新云录播") }
                        state.cloudVideos.take(10).forEach {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${it.title} (${it.status})", modifier = Modifier.weight(1f))
                                Button(onClick = { onOpenCloudVideo(it.videoId) }) { Text("播放") }
                            }
                        }
                        if (!state.cloudPlayUrl.isNullOrBlank()) {
                            Text("播放地址: ${state.cloudPlayUrl}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    MineVideoTabState.LOCAL -> {
                        Button(onClick = onPickLocalVideo) { Text("选择本地视频") }
                        Button(onClick = onTogglePlay) { Text(if (state.isVideoPlaying) "暂停/停止" else "播放") }
                        LocalVideoView(uriString = state.selectedVideoUri, isPlaying = state.isVideoPlaying)
                    }
                    MineVideoTabState.UPLOAD -> {
                        Button(onClick = onUploadPick) { Text("选择并上传MP4") }
                        Button(onClick = onPauseUpload) { Text("暂停上传") }
                        LinearProgressIndicator(progress = { state.uploadProgress }, modifier = Modifier.fillMaxWidth())
                        Text("上传状态: ${state.uploadStatus}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun MineVideoProfileCard(userName: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("U")
            }
            Column {
                Text(text = "UserAccount", style = MaterialTheme.typography.titleLarge)
                Text(text = if (userName.isBlank()) "Guest" else userName)
            }
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

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ComposeMineVideoActivityScreenPreview() {
    MagicVectorTheme {
        ComposeMineVideoActivityScreen(
            state = MineVideoState(userName = "Demo"),
            onSwitchVideoTab = {},
            onPickLocalVideo = {},
            onTogglePlay = {},
            onLoadCloudVideos = {},
            onOpenCloudVideo = {},
            onUploadPick = {},
            onPauseUpload = {}
        )
    }
}
