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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.magicvector.activity.ComposeLoginActivity
import com.magicvector.activity.test.ComposeTestActivity
import com.magicvector.viewModel.fragment.MineEffect
import com.magicvector.viewModel.fragment.MineIntent
import com.magicvector.viewModel.fragment.MineMainTab
import com.magicvector.viewModel.fragment.MineState
import com.magicvector.viewModel.fragment.MineVideoTab
import com.view.appview.R
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun MineScreen(
    modifier: Modifier = Modifier,
    viewModel: MineVm = MineVm()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.processIntent(MineIntent.OnLocalVideoSelected(uri?.toString()))
    }
    val uploadPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.processIntent(MineIntent.StartUpload(context.contentResolver, uri, "local-upload.mp4"))
        }
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
                MineEffect.NavigateToLogin -> {
                    context.startActivity(Intent(context, ComposeLoginActivity::class.java))
                }
                is MineEffect.ShowToast -> android.widget.Toast.makeText(context, effect.message, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    MineContent(
        modifier = modifier,
        state = state,
        onTestClick = { viewModel.processIntent(MineIntent.TestButtonClick) },
        onSwitchSetting = { viewModel.processIntent(MineIntent.SwitchToSetting) },
        onSwitchVideo = { viewModel.processIntent(MineIntent.SwitchToVideo) },
        onSwitchVideoTab = { viewModel.processIntent(MineIntent.SwitchVideoTab(it)) },
        onPickLocalVideo = { viewModel.processIntent(MineIntent.PickLocalVideoClick) },
        onTogglePlay = { viewModel.processIntent(MineIntent.ToggleVideoPlay) }
        ,
        onOldPasswordChange = { viewModel.processIntent(MineIntent.UpdateOldPassword(it)) },
        onNewPasswordChange = { viewModel.processIntent(MineIntent.UpdateNewPassword(it)) },
        onSubmitPassword = { viewModel.processIntent(MineIntent.SubmitPasswordUpdate) },
        onLogout = { viewModel.processIntent(MineIntent.Logout) },
        onLoadCloudVideos = { viewModel.processIntent(MineIntent.LoadCloudVideos) },
        onOpenCloudVideo = { id -> viewModel.processIntent(MineIntent.OpenCloudVideo(id)) },
        onUploadPick = { uploadPickerLauncher.launch("video/*") },
        onPauseUpload = { viewModel.processIntent(MineIntent.PauseUpload) }
    )
}


@Composable
fun MineContent(
    modifier: Modifier = Modifier,
    state: MineState,
    onTestClick: () -> Unit,
    onSwitchSetting: () -> Unit,
    onSwitchVideo: () -> Unit,
    onSwitchVideoTab: (MineVideoTab) -> Unit,
    onPickLocalVideo: () -> Unit,
    onTogglePlay: () -> Unit,
    onOldPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onSubmitPassword: () -> Unit,
    onLogout: () -> Unit,
    onLoadCloudVideos: () -> Unit,
    onOpenCloudVideo: (String) -> Unit,
    onUploadPick: () -> Unit,
    onPauseUpload: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        HeaderProfile(userName = state.userName)
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSwitchSetting) { Text("Setting") }
            Button(onClick = onSwitchVideo) { Text("视频") }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (state.tab == MineMainTab.SETTING) {
            SettingPanel(
                state = state,
                onOldPasswordChange = onOldPasswordChange,
                onNewPasswordChange = onNewPasswordChange,
                onSubmitPassword = onSubmitPassword,
                onLogout = onLogout
            )
        } else {
            VideoPanel(
                state = state,
                onSwitchVideoTab = onSwitchVideoTab,
                onPickLocalVideo = onPickLocalVideo,
                onTogglePlay = onTogglePlay,
                onLoadCloudVideos = onLoadCloudVideos,
                onOpenCloudVideo = onOpenCloudVideo,
                onUploadPick = onUploadPick,
                onPauseUpload = onPauseUpload
            )
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
private fun HeaderProfile(userName: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.foundation.layout.Box(
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
private fun SettingPanel(
    state: MineState,
    onOldPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onSubmitPassword: () -> Unit,
    onLogout: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Setting", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = state.oldPassword, onValueChange = onOldPasswordChange, label = { Text("旧密码") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = state.newPassword, onValueChange = onNewPasswordChange, label = { Text("新密码") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSubmitPassword) { Text("修改密码") }
                Button(onClick = onLogout) { Text("登出") }
            }
        }
    }
}

@Composable
private fun VideoPanel(
    state: MineState,
    onSwitchVideoTab: (MineVideoTab) -> Unit,
    onPickLocalVideo: () -> Unit,
    onTogglePlay: () -> Unit,
    onLoadCloudVideos: () -> Unit,
    onOpenCloudVideo: (String) -> Unit,
    onUploadPick: () -> Unit,
    onPauseUpload: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MineVideoTab.entries.forEach {
                    FilterChip(
                        selected = state.videoTab == it,
                        onClick = { onSwitchVideoTab(it) },
                        label = { Text(it.name) }
                    )
                }
            }
            when (state.videoTab) {
                MineVideoTab.CLOUD -> {
                    Button(onClick = onLoadCloudVideos) { Text("刷新云录播") }
                    state.cloudVideos.take(10).forEach {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${it.title} (${it.status})", modifier = Modifier.weight(1f))
                            Button(onClick = { onOpenCloudVideo(it.videoId) }) { Text("播放") }
                        }
                    }
                    if (!state.cloudPlayUrl.isNullOrBlank()) {
                        Text("播放地址: ${state.cloudPlayUrl}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                MineVideoTab.LOCAL -> {
                    Button(onClick = onPickLocalVideo) { Text("选择本地视频") }
                    Button(onClick = onTogglePlay) { Text(if (state.isVideoPlaying) "暂停/停止" else "播放") }
                    LocalVideoView(uriString = state.selectedVideoUri, isPlaying = state.isVideoPlaying)
                }
                MineVideoTab.UPLOAD -> {
                    Button(onClick = onUploadPick) { Text("选择并上传MP4") }
                    Button(onClick = onPauseUpload) { Text("暂停上传") }
                    LinearProgressIndicator(progress = { state.uploadProgress }, modifier = Modifier.fillMaxWidth())
                    Text("上传状态: ${state.uploadStatus}", style = MaterialTheme.typography.bodySmall)
                }
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


// 预览函数：直接预览 MineContent，传入模拟的点击事件
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun MineContentPreview() {
    MineContent(
        modifier = Modifier,
        state = MineState(userName = "Demo"),
        onTestClick = {},
        onSwitchSetting = {},
        onSwitchVideo = {},
        onSwitchVideoTab = {},
        onPickLocalVideo = {},
        onTogglePlay = {},
        onOldPasswordChange = {},
        onNewPasswordChange = {},
        onSubmitPassword = {},
        onLogout = {},
        onLoadCloudVideos = {},
        onOpenCloudVideo = {},
        onUploadPick = {},
        onPauseUpload = {}
    )
}
